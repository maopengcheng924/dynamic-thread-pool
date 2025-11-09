package config;

import com.alibaba.fastjson.JSON;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * PrometheusConfigRunner 启动
 * 在项目启动时自动扫描所有 ThreadPoolExecutor Bean，并将它们的核心运行指标注册给 Micrometer，供 Prometheus 拉取监控。
 */
@Slf4j
@Component
public class PrometheusConfigRunner implements ApplicationContextAware {
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @PostConstruct
    public void initPrometheus() {
        log.info("=== 开始注册线程池bean ===");
        try {
            String[] beanNamesForType = applicationContext.getBeanNamesForType(ThreadPoolExecutor.class);
            log.info("找到线程池beans {}", JSON.toJSONString(beanNamesForType));
            for (String beanName : beanNamesForType) {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) applicationContext.getBean(beanName);
                registerThreadPool(applicationContext.getEnvironment()
                                                     .getProperty("spring.application.name", "unknown"), beanName, executor);
            }
            log.info("=== 线程池注册完成 ===");
        } catch (Exception e) {
            log.error("注册线程池时发生异常", e);
        }
    }
    
    private void registerThreadPool(String applicationName, String poolName, ThreadPoolExecutor executor) {
        List<Tag> tags = Arrays.asList(
                new ImmutableTag("applicationName", applicationName),
                new ImmutableTag("poolName", poolName)
                                      );
        //Metrics.gauge(name, tags用于划分名称的维度序列, obj用于计算指标, valueFunction用于计算的函数)
        Metrics.gauge("thread_pool_core_size", tags, executor, ThreadPoolExecutor::getCorePoolSize);
        Metrics.gauge("thread_pool_max_size", tags, executor, ThreadPoolExecutor::getMaximumPoolSize);
        Metrics.gauge("thread_pool_active_thread_count", tags, executor, ThreadPoolExecutor::getActiveCount);
        Metrics.gauge("thread_pool_size", tags, executor, ThreadPoolExecutor::getPoolSize);
        Metrics.gauge("thread_pool_queue_size", tags, executor,
                      (threadPoolExecutor) -> threadPoolExecutor.getQueue()
                                                                .size());
        Metrics.gauge("thread_pool_queue_remaining_capacity", tags, executor,
                      (threadPoolExecutor) -> threadPoolExecutor.getQueue()
                                                                .remainingCapacity());
    }
}
