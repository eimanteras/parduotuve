package lt.eimantas.cdi;

import jakarta.enterprise.context.Dependent;
import java.math.BigDecimal;

@Dependent
@Audited
public class NumatytaPVMService implements PVMService {

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        return basePrice;
    }
}