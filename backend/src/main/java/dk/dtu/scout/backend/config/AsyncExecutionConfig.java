package dk.dtu.scout.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncExecutorProperties.class)
public class AsyncExecutionConfig {

    private final AsyncExecutorProperties props;

    public AsyncExecutionConfig(AsyncExecutorProperties props) {
        this.props = props;
    }

    @Bean(name = "requestExecutor")
    public Executor requestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("request-");
        executor.setCorePoolSize(props.getRequest().getCorePoolSize());
        executor.setMaxPoolSize(props.getRequest().getMaxPoolSize());
        executor.setQueueCapacity(props.getRequest().getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(props.getRequest().getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }

    @Bean(name = "runTaskExecutor")
    public Executor runExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("run-");
        executor.setCorePoolSize(props.getRun().getCorePoolSize());
        executor.setMaxPoolSize(props.getRun().getMaxPoolSize());
        executor.setQueueCapacity(props.getRun().getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(props.getRun().getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }
}
