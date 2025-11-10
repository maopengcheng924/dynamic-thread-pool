package cn.bugstack.middleware.dynamic.thread.pool.sdk.notify.strategy;


import cn.bugstack.middleware.dynamic.thread.pool.sdk.config.DynamicThreadPoolNotifyAutoProperties;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto.NotifyMessageDTO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.valobj.NotifyStrategyEnumVO;
import cn.bugstack.middleware.dynamic.thread.pool.sdk.notify.AbstractNotifyStrategy;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


/**
 * @author bing
 * @date 2025/7/1 13:51
 * @description 钉钉通知策略类
 */
@Slf4j
@Component
public class DingDingNotifyStrategy extends AbstractNotifyStrategy {
    
    private DynamicThreadPoolNotifyAutoProperties notifyProperties;
    
    public DingDingNotifyStrategy(DynamicThreadPoolNotifyAutoProperties notifyProperties) {
        this.notifyProperties = notifyProperties;
    }
    
    @Override
    public void sendNotify(
            NotifyMessageDTO notifyMsg) throws ApiException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        
        //获取配置中的token和secret信息
        String accessToken = notifyProperties.getAccessToken()
                                             .getDingDing();
        String secret = notifyProperties.getSecret()
                                        .getDingDing();
        
        //生成签名参数sign timestamp，为url使用
        //钉钉的加签机制
        Long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;    //生成签名字符串
        Mac mac = Mac.getInstance("HmacSHA256");        //加密算法实现类
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); //初始化加密器
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));   //进行加密，返回前面字节数组
        String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), StandardCharsets.UTF_8.name());
        
        //向钉钉发送消息
        //sign字段和timestamp字段拼接到请求url上，否则出现310000错误信息
        DefaultDingTalkClient dingTalkClient = new DefaultDingTalkClient(
                "https://oapi.dingtalk.com/robot/send?sign=" + sign + "&timestamp=" + timestamp);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        
        //定义文本消息
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(buildMsg(notifyMsg));
        
        //定义通知对象
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
        at.setIsAtAll(true);
        
        //配置消息类型
        request.setMsgtype("text");
        request.setText(text);
        request.setAt(at);
        
        //发送消息
        OapiRobotSendResponse response = dingTalkClient.execute(request, accessToken);
        
        if (!response.isSuccess()) {
            log.error("钉钉通知失败");
            throw new ApiException(response.getErrorCode(), response.getErrmsg());
        }
        log.info("钉钉通知成功");
    }
    
    
    @Override
    public String getStrategyName() {
        return NotifyStrategyEnumVO.DING_DING.getCode();
    }
}
