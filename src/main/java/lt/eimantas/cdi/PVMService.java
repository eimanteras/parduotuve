package lt.eimantas.cdi;

import java.math.BigDecimal;

public interface PVMService {
    BigDecimal calculatePrice(BigDecimal basePrice);
}