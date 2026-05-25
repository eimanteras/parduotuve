package lt.eimantas.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.math.BigDecimal;

@Alternative
@ApplicationScoped
public class SebMokejimas implements MokejimoServisas {

    @Override
    public void apmoketi(BigDecimal suma, String valiuta) {
        System.out.println(">>> [SEB] Sėkmingai apmokėta suma: " + suma + " " + valiuta);
    }
}