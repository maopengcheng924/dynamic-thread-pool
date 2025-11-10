package cn.bugstack.middleware.dynamic.thread.pool.sdk.domain;

import java.util.List;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

/**
 * @author bing
 * @date 2025/7/1 14:32
 * @description 告警服务接口
 */
public interface INotifyService {
    /**
     * 向告警平台通知线程池信息
     */
    void sendNotify(NotifyMessageDTO notifyMsg);
    
    /**
     * 向告警平台通知风险（如果发生）
     *
     * @param pools
     */
    void sendIfThreadPoolHasDanger(List<ThreadPoolConfigEntity> pools);
}
