package lt.eimantas.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Specializes;

@Specializes
@ApplicationScoped
public class SpecializuotasSkaiciavimoService extends SkaiciavimoService {

    @Override
    public String versija() {
        return "SPECIALIZED";
    }
}

