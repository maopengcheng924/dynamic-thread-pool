package cn.bugstack.middleware.dynamic.thread.pool.sdk.notify;

import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import com.taobao.api.ApiException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * @author bing
 * @date 2025/7/1 13:11
 * @description 通知策略抽象类
 */
public abstract class AbstractNotifyStrategy implements INotifyStrategy {
    
    public abstract String getStrategyName();
    
    //建立告警消息
    protected String buildMsg(NotifyMessageDTO notifyMsg) {
        StringBuilder content = new StringBuilder();
        HashMap<String, String> parameters = notifyMsg.getParameters();
        //拼接告警信息
        content.append("【动态线程池告警】")
               .append("\n")
               .append(notifyMsg.getMessage())
               .append("\n");
        parameters.forEach((k, v) -> content
                                             .append(" ")
                                             .append(k)
                                             .append(v)
                                             .append("\n"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        content.append("⏰通知时间：")
               .append(LocalDateTime.now()
                                    .format(formatter))
               .append("\n");
        return content.toString();
    }
    
    //具体通知方法由各种策略子类实现 这里是抽象方法
    public abstract void sendNotify(
            NotifyMessageDTO notifyMsg) throws ApiException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException;
}
