package lt.eimantas.async;

public class TaskState {
    private final String status;
    private final int progress; // ← Naujas laukas (nuo 0 iki 100)

    public TaskState(String status, int progress) {
        this.status = status;
        this.progress = progress;
    }

    public String getStatus() { return status; }
    public int getProgress() { return progress; }
}