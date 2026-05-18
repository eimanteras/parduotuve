package lt.eimantas.cdi;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.BadRequestException;
import lt.eimantas.rest.ProduktasDto; // priderink prie savo paketo
import java.math.BigDecimal;

@Interceptor
@MaxPriceCheck // Susiejame interceptorių su mūsų sukurta anotacija
@Priority(Interceptor.Priority.APPLICATION + 20) // Aktyvuojame globaliai
public class MaxPriceCheckInterceptor {

    @AroundInvoke
    public Object validatePrice(InvocationContext context) throws Exception {
        // 1. Pasiimame visus parametrus, kuriuos gavo REST metodas
        Object[] parameters = context.getParameters();

        // 2. Surandame, kuris iš parametrų yra mūsų ProduktasDto
        for (Object param : parameters) {
            if (param instanceof ProduktasDto) {
                ProduktasDto dto = (ProduktasDto) param;
                
                // 3. Atliekame validaciją (Jei kaina yra ir ji didesnė už 1000)
                if (dto.getKaina() != null && dto.getKaina().compareTo(new BigDecimal("1000")) > 0) {
                    // Svarbiausia vieta: metam klaidą ir NEKVIEČIAME context.proceed()
                    throw new BadRequestException("Kritinė klaida: Kaina negali viršyti 1000 eurų!");
                }
            }
        }

        // 4. Jei viskas gerai, leidžiame metodui vykdytis toliau
        return context.proceed();
    }
}