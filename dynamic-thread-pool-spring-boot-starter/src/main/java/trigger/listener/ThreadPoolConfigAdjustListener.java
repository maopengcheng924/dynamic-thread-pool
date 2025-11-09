package trigger.listener;

import java.util.List;

import service.IDynamicThreadPoolService;
import entity.ThreadPoolConfigEntity;
import service.IRegistry;
import org.redisson.api.listener.MessageListener;

/**
 * 线程池配置调整监听器
 * 监听Redis中的配置变更消息，实现线程池参数的动态调整
 */
public class ThreadPoolConfigAdjustListener implements MessageListener<ThreadPoolConfigEntity> {
    // 动态线程池服务接口，用于更新线程池配置
    private final IDynamicThreadPoolService dynamicThreadPoolService;
    
    // 注册中心接口，用于上报更新后的配置信息
    private final IRegistry registry;
    
    /**
     * 构造函数
     * @param dynamicThreadPoolService 动态线程池服务实例
     * @param registry 注册中心实例
     */
    public ThreadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }
    
    /**
     * 消息监听回调方法
     * 当Redis中收到线程池配置变更消息时触发此方法
     * @param charSequence 消息频道名称
     * @param threadPoolConfigEntity 新的线程池配置实体
     */
    @Override
    public void onMessage(CharSequence charSequence, ThreadPoolConfigEntity threadPoolConfigEntity) {
        // 更新本地线程池配置
        dynamicThreadPoolService.updateThreadPoolConfig(threadPoolConfigEntity);
        
        // 更新后上报最新数据
        // 获取所有线程池的最新配置列表
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
        // 上报线程池列表信息到注册中心
        registry.reportThreadPool(threadPoolConfigEntities);
        // 查询被更新线程池的当前配置
        ThreadPoolConfigEntity threadPoolConfigEntityCurrent =
                dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolConfigEntity.getThreadPoolName());
        // 上报被更新线程池的详细配置参数到注册中心
        registry.reportThreadPoolConfigParameter(threadPoolConfigEntityCurrent);
    }
}
