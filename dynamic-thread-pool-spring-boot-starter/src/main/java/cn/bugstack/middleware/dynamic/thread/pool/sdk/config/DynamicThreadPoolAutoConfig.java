package cn.bugstack.middleware.dynamic.thread.pool.sdk.config;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.NotifyService;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.notify.AbstractNotifyStrategy;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.registry.redis.RedisRegistry;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.trigger.job.ThreadPoolDataReportJob;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.trigger.listener.ThreadPoolConfigAdjustListener;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 动态配置入口
 */
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@EnableScheduling
@Configuration
public class DynamicThreadPoolAutoConfig {
    
    private final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolAutoConfig.class);
    
    private String applicationName;
    
    @Bean("redissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();
        //根据需要可以设定编解码器 https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);
        
        config.useSingleServer()
              .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
              .setPassword(properties.getPassword())
              .setConnectionPoolSize(properties.getPoolSize())
              .setConnectionMinimumIdleSize(properties.getMinIdleSize())
              .setIdleConnectionTimeout(properties.getIdleTimeout())
              .setConnectTimeout(properties.getConnectTimeout())
              .setRetryAttempts(properties.getRetryAttempts())
              .setRetryInterval(properties.getRetryInterval())
              .setPingConnectionInterval(properties.getPingInterval())
              .setKeepAlive(properties.isKeepAlive());
        
        RedissonClient redissonClient = Redisson.create(config);
        
        logger.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", properties.getHost(), properties.getPoolSize(),
                    !redissonClient.isShutdown());
        
        return redissonClient;
    }
    
    @Bean
    public IRegistry redisRegistry(RedissonClient redissonClient) {
        return new RedisRegistry(redissonClient);
    }
    
    @Bean("dynamicThreadPoolService")
    public DynamicThreadPoolService dynamicThreadPoolService(ApplicationContext applicationContext,
                                                             Map<String, ThreadPoolExecutor> threadPoolExecutorMap,
                                                             RedissonClient redissonClient) {
        applicationName = applicationContext.getEnvironment()
                                            .getProperty("spring.application.name");
        
        if (StringUtils.isBlank(applicationName)) {
            applicationName = "缺省的";
            logger.warn("动态线程池，启动提示，SpringBoot应用未配置 spring.application.name 无法获取到应用名称！");
        }
        
        if (StringUtil.isBlank(applicationName)) {
            logger.warn("应用程序名称为 null。您确定已配置应用程序吗？");
        }
        
        //根据 Redis 中缓存的配置 初始化线程池的参数，从而支持运行时的线程池动态调整。
        Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
        for (String threadPoolKey : threadPoolKeys) {
            //从 Redis 中拿出 key 为 "线程池配置前缀_应用名_线程池Key" 的对象，并反序列化成 ThreadPoolConfigEntity 对象。
            ThreadPoolConfigEntity threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(
                                                                                  RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey()
                                                                                  + "_" + applicationName + "_" + threadPoolKey)
                                                                          .get();
            if (null == threadPoolConfigEntity) continue;//如果redis里没有配置，threadPoolConfigEntity为null，只能使用默认配置
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
            threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaxPoolSize());
            threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
        }
        
        return new DynamicThreadPoolService(applicationName, threadPoolExecutorMap);
    }
    
    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService,
                                                           IRegistry registry) {
        return new ThreadPoolDataReportJob(dynamicThreadPoolService, registry);
    }
    
    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(
            IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry);
    }
    
    /**
     * 创建一个用于监听ThreadPoolConfigEntity变化的主题RTopic bean
     *
     * @param redissonClient                 Redis客户端，用于实现与redis的通信
     * @param threadPoolConfigAdjustListener 线程池配置修改监听处理器，用于监听变化，并且进行响应处理
     * @return RTopic 用于 Redis 的发布/订阅（Pub/Sub）
     */
    @Bean(name = "dynamicThreadPoolRedisTopic")
    public RTopic threadPoolConfigAdjustListener(RedissonClient redissonClient,
                                                 ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener) {
        RTopic topic = redissonClient.getTopic(
                RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + applicationName);
        topic.addListener(ThreadPoolConfigEntity.class, threadPoolConfigAdjustListener);
        return topic; //把这个 Topic 注册成 Spring Bean，后续你也可以拿它出来主动发布消息。
        //RTopic（频道） 是 Redisson 提供的一个类，用于 Redis 的发布/订阅（Pub/Sub）功能的封装。
    }
    
    /**
     * 注册告警通知服务类 bean
     */
    @Bean("notifyService")
    public NotifyService notifyService(DynamicThreadPoolNotifyAutoProperties properties, RedissonClient redissonClient,
                                       List<AbstractNotifyStrategy> strategyList) {
        return new NotifyService(properties, redissonClient, strategyList);
    }
    
}
