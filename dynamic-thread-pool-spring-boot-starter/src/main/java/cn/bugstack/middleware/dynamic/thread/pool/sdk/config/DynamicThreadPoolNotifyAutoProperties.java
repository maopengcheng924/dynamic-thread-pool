package cn.bugstack.middleware.dynamic.thread.pool.sdk.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bing
 * @date 2025/7/1 12:46
 * @description 配置动态线程池告警通知相关属性
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "dynamic.thread.pool.notify", ignoreInvalidFields = true)
public class DynamicThreadPoolNotifyAutoProperties {
    private Boolean enabled = false;
    private List<String> userPlatform = new ArrayList<>();
    private AccessToken accessToken;
    private Secret secret;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccessToken {
        private String dingDing;
        private String feiShu;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Secret {
        private String dingDing;
        private String feiShu;
    }
}
