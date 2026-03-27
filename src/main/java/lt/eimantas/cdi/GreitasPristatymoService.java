package lt.eimantas.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
public class GreitasPristatymoService implements PristatymoService {

    @Override
    public String tipas() {
        return "GREITAS_ALTERNATIVE";
    }
}

