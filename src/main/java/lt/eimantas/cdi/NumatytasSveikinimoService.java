package lt.eimantas.cdi;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Audited
public class NumatytasSveikinimoService implements SveikinimoService {

    @Override
    public String suformuoti(String vardas) {
        return "Labas, " + vardas;
    }
}

