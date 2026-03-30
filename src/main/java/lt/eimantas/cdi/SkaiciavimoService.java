package lt.eimantas.cdi;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SkaiciavimoService {

    public String versija() {
        return "BAZINIS";
    }
}

