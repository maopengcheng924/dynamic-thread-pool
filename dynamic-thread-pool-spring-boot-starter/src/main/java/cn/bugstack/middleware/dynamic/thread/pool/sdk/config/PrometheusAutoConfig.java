package cn.bugstack.middleware.dynamic.thread.pool.sdk.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
//import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bing
 * @date 2025/6/30 17:34
 * @description Prometheus自动配置类 配置指标过滤器，其余配置在测试端的application.yaml里写好。
 */

//@Slf4j
@Configuration
public class PrometheusAutoConfig {
    
    @Bean
    public MeterFilter customMeterFilter() {
        //Micrometer 的过滤器机制，可以用来筛选、修改、拦截、重命名某些 metrics。
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                //只允许采集名称中包含 "thread_pool" 的指标
                if (id.getName()
                      .contains("thread_pool")) {
                    return MeterFilterReply.ACCEPT;
                }
                return MeterFilterReply.DENY;
            }
        };
    }
}
