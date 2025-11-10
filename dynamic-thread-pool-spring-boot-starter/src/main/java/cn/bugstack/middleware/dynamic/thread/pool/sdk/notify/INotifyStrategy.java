package cn.bugstack.middleware.dynamic.thread.pool.sdk.notify;


import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import com.taobao.api.ApiException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author bing
 * @date 2025/7/1 13:03
 * @description 通知策略接口
 */
public interface INotifyStrategy {
    
    void sendNotify(
            NotifyMessageDTO notifyMsg) throws ApiException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException;
    
    String getStrategyName();
}
