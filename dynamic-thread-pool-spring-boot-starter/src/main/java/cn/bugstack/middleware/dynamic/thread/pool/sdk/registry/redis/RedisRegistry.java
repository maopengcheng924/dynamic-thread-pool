package cn.bugstack.middleware.dynamic.thread.pool.sdk.registry.redis;

import java.time.Duration;
import java.util.List;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;

/**
 * 基于Redis的注册中心实现类
 * 负责将线程池的配置信息和运行状态上报到Redis中
 */
public class RedisRegistry implements IRegistry {
    
    // Redisson客户端实例，用于与Redis进行交互
    private RedissonClient redissonClient;
    
    /**
     * 构造函数
     * @param redissonClient Redisson客户端实例
     */
    public RedisRegistry(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    /**
     * 上报线程池列表信息
     * 将所有线程池的基本配置信息存储到Redis的列表中
     * @param threadPoolEntities 线程池配置实体列表
     */
    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities) {
        // 获取Redis中的线程池配置列表
        RList<ThreadPoolConfigEntity> list = redissonClient.getList(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        
        // 将线程池配置信息批量添加到列表中
        list.addAll(threadPoolEntities);
    }
    
    /**
     * 上报单个线程池的详细配置参数
     * 将线程池的具体配置参数存储到Redis的Bucket中，并设置30天过期时间
     * @param threadPoolConfigEntity 线程池配置实体
     */
    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        // 构建缓存key，格式为：枚举key_应用名_线程池名
        String cacheKey = RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() +
                          "_" + threadPoolConfigEntity.getAppName() +
                          "_" + threadPoolConfigEntity.getThreadPoolName();
        
        // 获取Redis中的Bucket对象
        RBucket<ThreadPoolConfigEntity> bucket = redissonClient.getBucket(cacheKey);
        
        // 将线程池配置参数存储到Bucket中，并设置30天的过期时间
        bucket.set(threadPoolConfigEntity, Duration.ofDays(30));
    }
}
