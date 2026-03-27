package lt.eimantas.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.concurrent.ManagedExecutorService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AsyncTaskService {

    @Resource
    private ManagedExecutorService managedExecutorService;

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();

    public String startLongTask(int sleepSeconds) {
        String taskId = UUID.randomUUID().toString();
        TaskState state = new TaskState("RUNNING", "Pradeta", Instant.now().toString(), null);
        tasks.put(taskId, state);

        managedExecutorService.submit(() -> {
            try {
                Thread.sleep(Math.max(1, sleepSeconds) * 1000L);
                tasks.put(taskId, new TaskState("DONE", "Pabaigta", state.getStartedAt(), Instant.now().toString()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                tasks.put(taskId, new TaskState("FAILED", "Nutraukta", state.getStartedAt(), Instant.now().toString()));
            } catch (Exception ex) {
                tasks.put(taskId, new TaskState("FAILED", ex.getMessage(), state.getStartedAt(), Instant.now().toString()));
            }
        });

        return taskId;
    }

    public TaskState getTaskState(String taskId) {
        return tasks.get(taskId);
    }

    public static class TaskState {
        private String status;
        private String message;
        private String startedAt;
        private String finishedAt;

        public TaskState() {
        }

        public TaskState(String status, String message, String startedAt, String finishedAt) {
            this.status = status;
            this.message = message;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(String startedAt) {
            this.startedAt = startedAt;
        }

        public String getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(String finishedAt) {
            this.finishedAt = finishedAt;
        }
    }
}

