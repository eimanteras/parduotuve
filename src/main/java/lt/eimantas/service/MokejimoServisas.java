package lt.eimantas.service;

import java.math.BigDecimal;

public interface MokejimoServisas {
    // Užtikriname, kad interfeisas turi abu parametrus: suma ir valiuta
    void apmoketi(BigDecimal suma, String valiuta);
}