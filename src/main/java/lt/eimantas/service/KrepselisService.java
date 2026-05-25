package lt.eimantas.service;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import lt.eimantas.entity.Produktas;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SessionScoped
public class KrepselisService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Long> produktuIdKrepselyje = new ArrayList<>();

    @Inject
    private ParduotuveService parduotuveService;

    @Inject
    private MokejimoServisas mokejimoServisas;

    public void pridetiProdukta(Long produktoId) {
        if (produktoId == null) {
            throw new IllegalArgumentException("Produkto ID negali būti null");
        }
        produktuIdKrepselyje.add(produktoId);
    }

    public List<Produktas> getProduktai() {
        if (parduotuveService == null) {
            return Collections.emptyList();
        }
        return parduotuveService.getProduktaiByIds(produktuIdKrepselyje);
    }

    public BigDecimal skaiciuotiBendraSumaEur() {
        return getProduktai().stream()
                .map(Produktas::getKaina)
                .filter(kaina -> kaina != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pirkti(String valiuta) {
        if (produktuIdKrepselyje.isEmpty()) {
            throw new IllegalStateException("Krepšelis tuščias!");
        }

        String parinktaValiuta = (valiuta == null || valiuta.isBlank())
                ? "EUR"
                : valiuta.trim().toUpperCase(Locale.ROOT);

        BigDecimal sumaEur = skaiciuotiBendraSumaEur();
        BigDecimal galutineSuma = konvertuotiSuma(sumaEur, parinktaValiuta);

        if (mokejimoServisas == null) {
            throw new IllegalStateException("Mokėjimo servisas neinicijuotas");
        }

        mokejimoServisas.apmoketi(galutineSuma, parinktaValiuta);
        produktuIdKrepselyje.clear();
        return galutineSuma;
    }

    public void isvalyti() {
        produktuIdKrepselyje.clear();
    }

    private BigDecimal konvertuotiSuma(BigDecimal sumaEur, String valiuta) {
        switch (valiuta) {
            case "USD":
                return sumaEur.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);
            case "UAH":
                return sumaEur.multiply(new BigDecimal("43.00")).setScale(2, RoundingMode.HALF_UP);
            case "EUR":
                return sumaEur.setScale(2, RoundingMode.HALF_UP);
            default:
                throw new IllegalArgumentException("Nepalaikoma valiuta: " + valiuta);
        }
    }
}