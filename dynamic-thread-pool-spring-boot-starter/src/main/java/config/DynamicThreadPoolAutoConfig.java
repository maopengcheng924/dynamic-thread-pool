package config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import service.Impl.DynamicThreadPoolService;
import service.IDynamicThreadPoolService;
import entity.ThreadPoolConfigEntity;
import valobj.RegistryEnumVO;
import service.IRegistry;
import service.Impl.Registry;
import trigger.job.ThreadPoolDataReportJob;
import trigger.listener.ThreadPoolConfigAdjustListener;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 核心自动配置类，负责初始化整个动态线程池系统
 * 创建Redisson客户端、注册中心、线程池服务等核心Bean
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@EnableScheduling
public class DynamicThreadPoolAutoConfig {
    
    private   String applicationName;
    
    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();
        // 根据需要可以设定编解码器；https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
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
              .setKeepAlive(properties.isKeepAlive())
        ;
        
        RedissonClient redissonClient = Redisson.create(config);
        
        log.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}");
        
        return redissonClient;
    }
    @Bean
    public IRegistry redisRegistry(RedissonClient redissonClient){
        return new Registry(redissonClient);
    }
    
    @Bean("dynammicThreadPoolService")
    public DynamicThreadPoolService dynammicThreadPoolService(ApplicationContext applicationContext, Map<String,ThreadPoolExecutor> threadPoolExecutorMap,RedissonClient  redissonClient){
        
        applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if(StringUtils.isBlank(applicationName)){
            log.info("applicationName is null");
        }
        log.info("线程池信息");
        log.info(JSON.toJSONString(threadPoolExecutorMap.keySet()));
        
        Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
        for (String threadPoolKey : threadPoolKeys) {
            ThreadPoolConfigEntity threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + applicationName + "_" + threadPoolKey).get();
            if (null == threadPoolConfigEntity) continue;
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
            threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
            threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
        }
        
        return new DynamicThreadPoolService(applicationName,threadPoolExecutorMap);
    }
    
    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry){
        return new ThreadPoolDataReportJob(dynamicThreadPoolService,registry);
    }
    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry){
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService,registry);
    }
    @Bean(name="dynamicThreadPoolRedisTopic")
    public RTopic threadPoolConfigAdjustListener(RedissonClient redissonClient, ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener){
        RTopic topic = redissonClient.getTopic(RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + applicationName);
        topic.addListener(ThreadPoolConfigEntity.class,threadPoolConfigAdjustListener );
        return topic;
    }
    
    
}
