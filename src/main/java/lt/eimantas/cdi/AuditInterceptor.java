package lt.eimantas.cdi;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Response; // Reikia šio importo

@Interceptor
@Audited // priderink prie savo anotacijos pavadinimo
@Priority(Interceptor.Priority.APPLICATION + 10)
public class AuditInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        // 1. Spausdiname informaciją PRIEŠ metodo vykdymą
        System.out.println(">>> [AUDITAS] Kviečiamas metodas: " + context.getMethod().getName());

        // 2. Paleidžiame patį metodą ir IŠSISAUGOME jo grąžinamą rezultatą
        Object rezultatas = context.proceed();

        // 3. Spausdiname informaciją PO metodo vykdymo
        if (rezultatas != null) {
            System.out.println("<<< [AUDITAS] Metodas grąžino tipą: " + rezultatas.getClass().getSimpleName());
    
            // PATAISYTA: Tikriname ir iškart spausdiname tiesiogiai, 
            // nesukurdami lokalaus kintamojo, kurį IDE reikalautų uždaryti.
            if (rezultatas instanceof Response) {
                System.out.println("<<< [AUDITAS] HTTP Statuso kodas: " + ((Response) rezultatas).getStatus());
        }
        } else {
            System.out.println("<<< [AUDITAS] Metodas nieko negrąžino (void).");
        }


        // 5. Privalome grąžinti rezultatą atgal gijai!
        return rezultatas;
    }
}