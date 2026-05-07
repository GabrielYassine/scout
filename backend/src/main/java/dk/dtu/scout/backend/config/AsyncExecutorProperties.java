package dk.dtu.scout.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds executor pool settings from application.properties.
 * Spring fills this class automatically through @ConfigurationProperties,
 * so the setters are used by Spring even though they are not called directly in the code.
 * @author Ahmed
 */
@ConfigurationProperties(prefix = "scout.executors")
public class AsyncExecutorProperties {

    private final ExecutorSpec request = new ExecutorSpec();
    private final ExecutorSpec run = new ExecutorSpec();

    public ExecutorSpec getRequest() {
        return request;
    }

    public ExecutorSpec getRun() {
        return run;
    }

    public static class ExecutorSpec {
        private int corePoolSize;
        private int maxPoolSize;
        private int queueCapacity;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
}