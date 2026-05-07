package dk.dtu.scout.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures the executor pools used for asynchronous backend work.
 * The actual pool sizes and queue limits are read from application.properties
 * through AsyncExecutorProperties.
 * requestExecutor is used for higher-level request orchestration.
 * runTaskExecutor is used for executing individual algorithm runs in parallel.
 * @author Ahmed
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncExecutorProperties.class)
public class AsyncExecutionConfig {

    private final AsyncExecutorProperties props;

    public AsyncExecutionConfig(AsyncExecutorProperties props) {
        this.props = props;
    }

    /**
     * Creates the executor used for high-level request orchestration.
     * This executor is used by RunOrchestratorService to start and monitor run or runtime-study tasks.
     * @return configured request executor bean
     */
    @Bean(name = "requestExecutor")
    public Executor requestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("request-");
        executor.setCorePoolSize(props.getRequest().getCorePoolSize());
        executor.setMaxPoolSize(props.getRequest().getMaxPoolSize());
        executor.setQueueCapacity(props.getRequest().getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Creates the executor used for executing algorithm runs.
     * This executor is used by RunExecutor to run repeated runtimes in parallel.
     * @return configured run executor bean
     */
    @Bean(name = "runTaskExecutor")
    public Executor runExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("run-");
        executor.setCorePoolSize(props.getRun().getCorePoolSize());
        executor.setMaxPoolSize(props.getRun().getMaxPoolSize());
        executor.setQueueCapacity(props.getRun().getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}