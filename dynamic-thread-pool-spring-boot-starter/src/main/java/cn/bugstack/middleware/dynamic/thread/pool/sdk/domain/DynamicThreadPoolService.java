package cn.bugstack.middleware.dynamic.thread.pool.sdk.domain;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class DynamicThreadPoolService implements IDynamicThreadPoolService {
    
    private final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolService.class);
    private final String applicationName;
    private final Map<String, ThreadPoolExecutor> threadPoolExecutorMap;
    
    @Resource
    private INotifyService notifyService;
    
    public DynamicThreadPoolService(String applicationName, Map<String, ThreadPoolExecutor> threadPoolExecutorMap) {
        this.applicationName = applicationName;
        this.threadPoolExecutorMap = threadPoolExecutorMap;
    }
    
    
    @Override
    public List<ThreadPoolConfigEntity> queryThreadPoolList() {
        Set<String> threadPoolBeanNames = threadPoolExecutorMap.keySet();
        List<ThreadPoolConfigEntity> threadPoolVOS = new ArrayList<>(threadPoolBeanNames.size());
        for (String beanName : threadPoolBeanNames) {
            ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, beanName);
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(beanName);
            threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
            threadPoolConfigVO.setMaxPoolSize(threadPoolExecutor.getMaximumPoolSize());
            threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
            threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
            threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue()
                                                              .getClass()
                                                              .getSimpleName());
            threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue()
                                                              .size());
            threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue()
                                                                      .remainingCapacity());
            threadPoolVOS.add(threadPoolConfigVO);
        }
        return threadPoolVOS;
    }
    
    @Override
    public ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName) {
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolName);
        //检查线程池是否存在
        if (null == threadPoolExecutor) return new ThreadPoolConfigEntity(applicationName, threadPoolName);
        
        //线程池配置数据
        ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, threadPoolName);
        threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        threadPoolConfigVO.setMaxPoolSize(threadPoolExecutor.getMaximumPoolSize());
        threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
        threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
        threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue()
                                                          .getClass()
                                                          .getSimpleName());
        threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue()
                                                          .size());
        threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue()
                                                                  .remainingCapacity());
        
        if (logger.isDebugEnabled()) {
            logger.info("动态线程池，配置查询 应用名：{} 线程名：{} 池化配置：{}", applicationName, threadPoolName,
                        JSON.toJSONString(threadPoolConfigVO));
        }
        
        return threadPoolConfigVO;
    }
    
    @Override
    public void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity) {
        //检配置实体 app名 线程名是否为空
        if (null == threadPoolConfigEntity || !applicationName.equals(threadPoolConfigEntity.getAppName())) return;
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolConfigEntity.getThreadPoolName());
        if (null == threadPoolExecutor) return;
        
        //设置参数 [调整核心线程数和最大线程数]
        threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
        threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaxPoolSize());
        
        //线程池配置修改时，生成msg，调用通知服务的告警功能，发送通知
        NotifyMessageDTO messageDTO = new NotifyMessageDTO();
        messageDTO.setMessage("\uD83D\uDD14线程池配置更新通知\uD83D\uDD14");
        messageDTO.addParameter("应用名称: ", threadPoolConfigEntity.getAppName())
                  .addParameter("线程池名称: ", threadPoolConfigEntity.getThreadPoolName())
                  .addParameter("核心线程数: ", threadPoolConfigEntity.getCorePoolSize())
                  .addParameter("最大线程数: ", threadPoolConfigEntity.getMaxPoolSize());
        notifyService.sendNotify(messageDTO);
    }
}
