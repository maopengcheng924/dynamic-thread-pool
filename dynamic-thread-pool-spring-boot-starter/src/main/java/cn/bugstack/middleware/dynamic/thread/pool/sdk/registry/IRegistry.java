package cn.bugstack.middleware.dynamic.thread.pool.sdk.registry;

import java.util.List;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

public interface IRegistry {
    
    void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities);
    
    void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity);
}
