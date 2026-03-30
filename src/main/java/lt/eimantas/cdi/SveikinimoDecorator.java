package lt.eimantas.cdi;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

@Decorator
public abstract class SveikinimoDecorator implements SveikinimoService {

    @Inject
    @Delegate
    private SveikinimoService delegate;

    @Override
    public String suformuoti(String vardas) {
        return delegate.suformuoti(vardas) + " (dekoruota)";
    }
}

