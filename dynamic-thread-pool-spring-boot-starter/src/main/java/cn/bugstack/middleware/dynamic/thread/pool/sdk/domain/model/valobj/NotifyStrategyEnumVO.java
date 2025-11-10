package cn.bugstack.middleware.dynamic.thread.pool.sdk.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author bing
 * @date 2025/7/1 12:59
 * @description 通知策略枚举类
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NotifyStrategyEnumVO {
    
    //两种枚举策略代码和描述
    
    DING_DING("DingDing", "钉钉"),
    FEI_SHU("FeiShu", "飞书");
    
    private String code;
    private String desc;
}
