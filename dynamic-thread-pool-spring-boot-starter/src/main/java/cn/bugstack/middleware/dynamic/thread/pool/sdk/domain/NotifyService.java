package cn.bugstack.middleware.dynamic.thread.pool.sdk.domain;


import cn.bugstack.middleware.dynamic.thread.pool.sdk.config.DynamicThreadPoolNotifyAutoProperties;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.notify.AbstractNotifyStrategy;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bing
 * @date 2025/7/1 14:35
 * @description 告警服务实现类
 */
@Slf4j
public class NotifyService implements INotifyService {
    private final DynamicThreadPoolNotifyAutoProperties properties;
    private final RedissonClient redissonClient;
    private Map<String, AbstractNotifyStrategy> strategies = new HashMap<>();
    
    public NotifyService(DynamicThreadPoolNotifyAutoProperties properties, RedissonClient redissonClient,
                         List<AbstractNotifyStrategy> strategyList) {
        this.properties = properties;
        this.redissonClient = redissonClient;
        //将传入的策略列表strategyList转为strategies map，key是策略名称
        this.strategies = strategyList.stream()
                                      .collect(Collectors.toMap(AbstractNotifyStrategy::getStrategyName,
                                                                strategy -> strategy));
    }
    
    //根据传入的策略名称List返回策略List
    public List<AbstractNotifyStrategy> selectStrategy(List<String> strategyNames) {
        return strategyNames.stream()
                            .map(strategies::get)
                            .collect(Collectors.toList());
    }
    
    @Override
    public void sendNotify(NotifyMessageDTO notifyMsg) {
        Boolean enabled = properties.getEnabled();
        List<String> platform = properties.getUserPlatform();
        if (!enabled || platform.isEmpty()) {
            log.warn("告警服务未启用");
            return;
        }
        
        //提前筛选出可用策略
        List<AbstractNotifyStrategy> selectedStrategies = selectStrategy(platform);
        if (selectedStrategies.isEmpty()) {
            log.warn("没有可用的告警策略");
            return;
        }
        
        //遍历策略发送通知
        for (AbstractNotifyStrategy strategy : selectedStrategies) {
            try {
                strategy.sendNotify(notifyMsg);
            } catch (Exception e) {
                log.error("告警平台：{} 发送告警失败", strategy.getStrategyName(), e);
            }
        }
        log.info("推送告警：{}", JSON.toJSONString(notifyMsg));
    }
    
    @Override
    public void sendIfThreadPoolHasDanger(List<ThreadPoolConfigEntity> pools) {
        List<ThreadPoolConfigEntity> dangerPools = new ArrayList<>();
        
        // 遍历线程池列表
        for (ThreadPoolConfigEntity pool : pools) {
            // 获取线程池当前状态
            int corePoolSize = pool.getCorePoolSize();
            int maximumPoolSize = pool.getMaxPoolSize();
            int activeCount = pool.getActiveCount();
            int queueSize = pool.getQueueSize();
            int remainingCapacity = pool.getRemainingCapacity();
            int totalCapacity = queueSize + remainingCapacity;
            
            // 定义危险条件
            boolean isThreadOverload = activeCount > corePoolSize * 0.8;   // 活跃线程数超过核心线程数的80%
            boolean isMaxThreadOverload = activeCount >= maximumPoolSize;  // 活跃线程数已经达到最大线程数
            boolean isQueueAlmostFull = queueSize >= 0.8 * totalCapacity; // 队列剩余容量不足20%
            
            // 创建一个变量来存储触发的告警条件
            StringBuilder dangerReason = new StringBuilder();
            
            // 检查并记录触发的告警条件
            if (isThreadOverload) {
                dangerReason.append("活跃线程数超过核心线程数的80%；");
            }
            if (isMaxThreadOverload) {
                dangerReason.append("活跃线程数已经达到最大线程数；");
            }
            if (isQueueAlmostFull) {
                dangerReason.append("队列剩余容量不足20%；");
            }
            
            // 如果满足任何一个危险条件，则将此线程池添加到危险列表中
            if (dangerReason.length() > 0) {
                pool.setAlarmReason(dangerReason.toString());
                dangerPools.add(pool);
                
                // 构建通知消息
                NotifyMessageDTO notifyMessageDTO = new NotifyMessageDTO();
                notifyMessageDTO.setMessage("\uD83D\uDD14线程池预警\uD83D\uDD14");
                notifyMessageDTO.addParameter("目前危险线程池数量为: ", dangerPools.size());
                notifyMessageDTO.addParameter("!告警原因!:", pool.getAlarmReason())
                                .addParameter("应用名称: ", pool.getAppName())
                                .addParameter("线程池名称: ", pool.getThreadPoolName())
                                .addParameter("当前线程数: ", pool.getPoolSize())
                                .addParameter("核心线程数: ", pool.getCorePoolSize())
                                .addParameter("最大线程数: ", pool.getMaxPoolSize())
                                .addParameter("活跃线程数: ", pool.getActiveCount())
                                .addParameter("队列类型: ", pool.getQueueType())
                                .addParameter("队列中任务数: ", pool.getQueueSize())
                                .addParameter("队列剩余容量: ", pool.getRemainingCapacity());
                
                // 发送告警
                sendNotifyWithRateLimit(notifyMessageDTO);
                // 打印告警信息
                log.debug("线程池告警信息: {}", JSON.toJSONString(notifyMessageDTO));
            }
        }
    }
    
    /**
     * 限流告警机制：通过分布式锁 + 告警计数 + TTL 冷却机制，实现告警频率控制 + 冷静期保护。
     */
    private void sendNotifyWithRateLimit(NotifyMessageDTO notifyMsg) {
        String appName = notifyMsg.getParameters()
                                  .get("应用名称: ");
        String threadPoolName = notifyMsg.getParameters()
                                         .get("线程池名称: ");
        String lockKey = "notifyLock:" + appName + "_" + threadPoolName;
        String counterKey = "notifyCounter:" + appName + "_" + threadPoolName;
        String ttlKey = "ttlCounter:" + appName + "_" + threadPoolName;
        
        RLock lock = redissonClient.getLock(lockKey);   //分布式锁：保证多服务节点下的同步
        try {
            lock.lock(10, TimeUnit.SECONDS); // 显式设置锁的持有时间
            
            RAtomicLong atomicLong = redissonClient.getAtomicLong(counterKey);  //计数器：告警频率统计
            RBucket<String> ttlBucket = redissonClient.getBucket(ttlKey);   //TTL：设置告警冷却时间
            
            long remainTimeToLive = ttlBucket.remainTimeToLive() / 1000;
            long currentCount = atomicLong.get();
            
            if (remainTimeToLive > 0) {
                log.warn("应用 {} 线程 {} 进入冷却期，下一次发送通知剩余时间: {} 秒。已连续通知 {} 次",
                         appName, threadPoolName, remainTimeToLive, currentCount);
                return;
            }
            
            if (currentCount == 0) {
                atomicLong.set(1);
                ttlBucket.set(appName + threadPoolName + " is locked for 5 minutes.", Duration.ofMinutes(5));
                sendNotify(notifyMsg);
            } else if (currentCount >= 3) {
                atomicLong.set(0);
                ttlBucket.set(appName + threadPoolName + " is locked for 2 hours.", Duration.ofHours(2));
                log.warn("应用 {} 线程 {} 连续3次告警，进入冷却期 2 小时。", appName, threadPoolName);
            } else {
                atomicLong.incrementAndGet();
                ttlBucket.set(appName + threadPoolName + " is locked for 5 minutes.", Duration.ofMinutes(5));
                sendNotify(notifyMsg);
            }
        } catch (Exception e) {
            log.error("发送通知时发生错误: ", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
