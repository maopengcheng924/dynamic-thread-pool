package cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

/**
 * @author bing
 * @date 2025/7/1 12:53
 * @description 告警信息(消息 + 告警参数)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotifyMessageDTO {
    
    private String message;
    private LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
    
    public <T> NotifyMessageDTO addParameter(String k, T v) { //用泛型接受各种类型参数，然后在方法里统一转为String
        parameters.put(k, String.valueOf(v));
        return this;
    }
}
