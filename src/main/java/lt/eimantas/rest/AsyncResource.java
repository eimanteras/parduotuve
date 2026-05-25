package lt.eimantas.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lt.eimantas.service.AsyncTaskService;

import java.util.Map;

@Path("/async")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AsyncResource {

    @Inject
    private AsyncTaskService asyncTaskService;

    @POST
    @Path("/tasks")
    public Response startTask(Map<String, Integer> payload) {
        int sleepSeconds = payload != null && payload.get("sleepSeconds") != null ? payload.get("sleepSeconds") : 5;
        String taskId = asyncTaskService.startLongTask(sleepSeconds);
        return Response.accepted(Map.of("taskId", taskId, "statusUrl", "/api/async/tasks/" + taskId)).build();
    }

    @GET
    @Path("/tasks/{taskId}")
    public AsyncTaskService.TaskState getTaskStatus(@PathParam("taskId") String taskId) {
        AsyncTaskService.TaskState state = asyncTaskService.getTaskState(taskId);
        if (state == null) {
            throw new NotFoundException("Uzduotis nerasta");
        }
        return state;
    }

    @DELETE
    @Path("/tasks/{taskId}")
    public Response cancelTask(@PathParam("taskId") String taskId) {
        boolean cancelled = asyncTaskService.cancelTask(taskId);
        if (!cancelled) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "TASK_NOT_FOUND",
                                   "message", "Užduotis nerasta arba jau pabaigta"))
                    .build();
        }
        return Response.ok(Map.of("taskId", taskId, "status", "CANCELLED")).build();
    }
}