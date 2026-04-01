package dk.dtu.scout.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
        private int corePoolSize = 4;
        private int maxPoolSize = 16;
        private int queueCapacity = 200;
        private int awaitTerminationSeconds = 30;

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

        public int getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }
}
