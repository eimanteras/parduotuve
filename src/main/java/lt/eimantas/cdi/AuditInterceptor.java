package lt.eimantas.cdi;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.annotation.Priority;

@Priority(Interceptor.Priority.APPLICATION + 10)
@Audited
@Interceptor
public class AuditInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        long started = System.currentTimeMillis();
        try {
            return context.proceed();
        } finally {
            long tookMs = System.currentTimeMillis() - started;
            System.out.println("[AuditInterceptor] " + context.getMethod().getName() + " took " + tookMs + " ms");
        }
    }
}

