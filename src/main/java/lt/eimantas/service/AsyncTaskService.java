package lt.eimantas.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.concurrent.ManagedExecutorService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@ApplicationScoped
public class AsyncTaskService {

    @Resource
    private ManagedExecutorService managedExecutorService;

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public String startLongTask(int sleepSeconds) {
        String taskId = UUID.randomUUID().toString();
        TaskState state = new TaskState("RUNNING", "Pradeta", Instant.now().toString(), null);
        tasks.put(taskId, state);

        // Pakeista: išsaugojamas grąžinamas Future objektas
        Future<?> future = managedExecutorService.submit(() -> {
            try {
                Thread.sleep(Math.max(1, sleepSeconds) * 1000L);
                tasks.put(taskId, new TaskState("DONE", "Pabaigta", state.getStartedAt(), Instant.now().toString()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                tasks.put(taskId, new TaskState("FAILED", "Nutraukta", state.getStartedAt(), Instant.now().toString()));
            } catch (Exception ex) {
                tasks.put(taskId, new TaskState("FAILED", ex.getMessage(), state.getStartedAt(), Instant.now().toString()));
            } finally {
                // Saugiklis: užduočiai pasibaigus natūraliai, pašaliname ją iš aktyvių gijų sekimo
                runningTasks.remove(taskId);
            }
        });

        runningTasks.put(taskId, future);
        return taskId;
    }

    public boolean cancelTask(String taskId) {
        Future<?> future = runningTasks.get(taskId);
        if (future == null) {
            return false;
        }
        future.cancel(true);
        runningTasks.remove(taskId);
        tasks.put(taskId, new TaskState(
            "FAILED",
            "Nutraukta vartotojo",
            tasks.containsKey(taskId) ? tasks.get(taskId).getStartedAt() : null,
            Instant.now().toString()
        ));
        return true;
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