package lt.eimantas.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class OptimisticConflictExceptionMapper implements ExceptionMapper<OptimisticConflictException> {

    @Override
    public Response toResponse(OptimisticConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "OPTIMISTIC_LOCK_CONFLICT",
                        "message", exception.getMessage()
                ))
                .build();
    }
}

