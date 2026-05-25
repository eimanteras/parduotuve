package lt.eimantas.cdi;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Decorator
public class PVMDecorator implements PVMService {

    @Inject
    @Delegate
    private PVMService delegate;

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        BigDecimal corePrice = delegate.calculatePrice(basePrice);
        BigDecimal vatMultiplier = new BigDecimal("1.21");
        return corePrice.multiply(vatMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}