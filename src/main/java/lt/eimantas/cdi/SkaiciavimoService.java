package lt.eimantas.cdi;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SkaiciavimoService {
    public double getDiscount() {
        return 1.0;
    }
}