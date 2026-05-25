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
    public Map<String, BigDecimal> produceExchangeRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("1.00"));
        rates.put("USD", new BigDecimal("1.10"));
        rates.put("UAH", new BigDecimal("43.00"));
        return rates;
    }
}