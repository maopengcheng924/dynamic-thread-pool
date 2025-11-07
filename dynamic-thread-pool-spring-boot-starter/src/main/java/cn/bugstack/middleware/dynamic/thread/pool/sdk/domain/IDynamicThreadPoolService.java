package cn.bugstack.middleware.dynamic.thread.pool.sdk.domain;

import java.util.List;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

public interface IDynamicThreadPoolService {

    List<ThreadPoolConfigEntity> queryThreadPoolList();
    
    ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName);
    
    void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity);

}
