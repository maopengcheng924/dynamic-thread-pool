package trigger.job;

import java.util.List;

import service.IDynamicThreadPoolService;
import entity.ThreadPoolConfigEntity;
import service.IRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 线程池数据上报任务类
 * 定时将应用中的线程池运行状态和配置信息上报到注册中心
 */
@Slf4j
public class ThreadPoolDataReportJob {
    // 动态线程池服务接口，用于查询线程池信息
    private final IDynamicThreadPoolService dynamicThreadPoolService;
    
    // 注册中心接口，用于上报线程池数据
    private final IRegistry registry;
    
    /**
     * 构造函数
     * @param dynamicThreadPoolService 动态线程池服务实例
     * @param registry 注册中心实例
     */
    public ThreadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }
    
    /**
     * 定时执行的线程池数据上报任务
     * 每30秒执行一次，将线程池列表和详细配置参数上报到注册中心
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void execReportThreadPoolList(){
        // 查询所有线程池的配置信息
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
        // 上报线程池列表信息到注册中心
        registry.reportThreadPool(threadPoolConfigEntities);
        // 记录线程池数据上报日志
        log.info("【线程池数据上报】{}", threadPoolConfigEntities);
        
        // 遍历每个线程池，上报其详细配置参数
        for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
            // 上报单个线程池的详细配置参数到注册中心
            registry.reportThreadPoolConfigParameter(threadPoolConfigEntity);
            // 记录线程池参数上报日志
            log.info("【线程池参数上报】{}", threadPoolConfigEntity);
        }
    }
    
}
