package service;

import java.util.List;

import entity.ThreadPoolConfigEntity;

public interface IDynamicThreadPoolService {

    List<ThreadPoolConfigEntity> queryThreadPoolList();
    
    ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName);
    
    void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity);

}
