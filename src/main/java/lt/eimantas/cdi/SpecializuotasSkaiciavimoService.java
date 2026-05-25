package lt.eimantas.cdi;

import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.context.Dependent;

@Dependent
@Specializes
public class SpecializuotasSkaiciavimoService extends SkaiciavimoService {

    @Override
    public double getDiscount() {
        return 0.90;
    }
}