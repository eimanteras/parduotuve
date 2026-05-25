package lt.eimantas.cdi;

import jakarta.enterprise.context.Dependent;
import java.math.BigDecimal;

@Dependent
@PristatymoTipas.Standartinis // Pažymime, kad tai standartinis būdas
public class StandartinisPristatymoService implements PristatymoService {

    @Override
    public BigDecimal getDeliveryPrice() {
        return new BigDecimal("3.99"); // Pigus pristatymas į paštomatą
    }

    @Override
    public String getDeliveryType() {
        return "STANDARD_POST";
    }
}