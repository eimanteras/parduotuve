package lt.eimantas.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;

@ApplicationScoped
public class SwedbankMokejimas implements MokejimoServisas {

    @Override
    public void apmoketi(BigDecimal suma, String valiuta) {
        System.out.println(">>> [SWEDBANK] Sėkmingai apmokėta suma: " + suma + " " + valiuta);
    }
}