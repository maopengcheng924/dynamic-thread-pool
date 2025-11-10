package cn.bugstack.middleware.dynamic.thread.pool.sdk.notify.strategy;


import cn.bugstack.middleware.dynamic.thread.pool.sdk.config.DynamicThreadPoolNotifyAutoProperties;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.valobj.NotifyStrategyEnumVO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.notify.AbstractNotifyStrategy;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author bing
 * @date 2025/7/1 14:28
 * @description 飞书通知策略类
 */
@Slf4j
@Component
public class FeiShuNotifyStrategy extends AbstractNotifyStrategy {
    
    private DynamicThreadPoolNotifyAutoProperties notifyProperties;
    
    public FeiShuNotifyStrategy(DynamicThreadPoolNotifyAutoProperties notifyProperties) {
        this.notifyProperties = notifyProperties;
    }
    
    @Override
    public String getStrategyName() {
        return NotifyStrategyEnumVO.FEI_SHU.getCode();
    }
    
    @Override
    public void sendNotify(
            NotifyMessageDTO notifyMsg) throws ApiException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        log.debug("飞书通知暂未实现。");
    }
}
