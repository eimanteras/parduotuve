package lt.eimantas.cdi;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Dependent
public class ValiutosKonverteris {

    @Produces
    @KursuZemelapis
    public Map<String, BigDecimal> produceKursai() {
        Map<String, BigDecimal> kursai = new HashMap<>();
        kursai.put("EUR", new BigDecimal("1.00"));
        kursai.put("USD", new BigDecimal("1.10"));
        kursai.put("UAH", new BigDecimal("43.00"));
        return kursai;
    }
}
