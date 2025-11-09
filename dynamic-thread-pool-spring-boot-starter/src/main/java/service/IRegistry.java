package service;

import java.util.List;

import entity.ThreadPoolConfigEntity;

public interface IRegistry {
    
    void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities);
    
    void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity);
}
