package lt.eimantas.cdi;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        long started = System.nanoTime(); // Naudojame nanosekundes tikslumui
        try {
            return context.proceed();
        } finally {
            long tookNs = System.nanoTime() - started;
            double tookMs = tookNs / 1_000_000.0; // Paverčiame į skaitomas milisekundes
            
            System.out.println(String.format("[AuditInterceptor] %s took %.3f ms", 
                    context.getMethod().getName(), tookMs));
        }
    }
}