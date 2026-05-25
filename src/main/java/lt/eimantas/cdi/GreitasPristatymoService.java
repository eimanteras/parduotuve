package lt.eimantas.cdi;

import jakarta.enterprise.context.Dependent;
import java.math.BigDecimal;

@Dependent
@PristatymoTipas.Greitas // Pažymime, kad tai greitasis būdas
public class GreitasPristatymoService implements PristatymoService {

    @Override
    public BigDecimal getDeliveryPrice() {
        return new BigDecimal("8.99"); // Brangus skubus kurjeris
    }

    @Override
    public String getDeliveryType() {
        return "EXPRESS_COURIER";
    }
}