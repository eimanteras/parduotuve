package lt.eimantas.cdi;

import java.math.BigDecimal;

public interface PristatymoService {
    BigDecimal getDeliveryPrice();
    String getDeliveryType();
}