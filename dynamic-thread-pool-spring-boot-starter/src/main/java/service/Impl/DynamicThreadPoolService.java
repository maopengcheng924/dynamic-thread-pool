package service.Impl;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import entity.ThreadPoolConfigEntity;
import service.IDynamicThreadPoolService;

/**
 * 提供线程池查询和更新的核心服务接口及实现
 * 支持查询线程池列表、根据名称查询特定线程池、更新线程池配置等功能
 */
public class DynamicThreadPoolService implements IDynamicThreadPoolService {
    
    
    private final Map<String, ThreadPoolExecutor> threadPoolExecutorMap;
    
    private final String applicationName;
    
    public DynamicThreadPoolService( String applicationName,Map<String, ThreadPoolExecutor> threadPoolExecutorMap) {
        this.threadPoolExecutorMap
                             = threadPoolExecutorMap;
        this.applicationName = applicationName;
    }
    
    @Override
    public List<ThreadPoolConfigEntity> queryThreadPoolList() {
        Set<String> threadPoolBeanNames = threadPoolExecutorMap.keySet();
        List<ThreadPoolConfigEntity>threadPoolVOS=new ArrayList<>(threadPoolBeanNames.size());
        for (String beanName : threadPoolBeanNames) {
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(beanName);
            ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, beanName);
            threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
            threadPoolConfigVO.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
            threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
            threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
            threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue().getClass().getName());
            threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue().size());
            threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());
            threadPoolVOS.add(threadPoolConfigVO);
        }
        return threadPoolVOS;
    }
    
    @Override
    public ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName) {
        ThreadPoolExecutor threadPoolExecutor=threadPoolExecutorMap.get(threadPoolName);
        if(null==threadPoolExecutor){
            return new ThreadPoolConfigEntity(applicationName,threadPoolName);
        }
        ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, threadPoolName);
        threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        threadPoolConfigVO.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
        threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
        threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue().getClass().getName());
        threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue().size());
        threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());
        return threadPoolConfigVO;
    }
    
    @Override
    public void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity) {
        if(null==threadPoolConfigEntity||!applicationName.equals(threadPoolConfigEntity.getAppName())) return;
        ThreadPoolExecutor threadPoolExecutor=threadPoolExecutorMap.get(threadPoolConfigEntity.getThreadPoolName());
        
        if(null==threadPoolExecutor) return;
        threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
        threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
        
    }
}
