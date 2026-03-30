package lt.eimantas.cdi;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StandartinisPristatymoService implements PristatymoService {

    @Override
    public String tipas() {
        return "STANDARTINIS";
    }
}

