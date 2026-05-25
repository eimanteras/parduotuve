package lt.eimantas.service;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import lt.eimantas.cdi.KursuZemelapis;
import lt.eimantas.entity.Produktas;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SessionScoped
public class KrepselisService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Long> produktuIdKrepselyje = new ArrayList<>();

    @Inject
    private ParduotuveService parduotuveService;

    @Inject
    private MokejimoServisas mokejimoServisas;

    // Pakeitimas: Įšvirkščiame dinaminį kursų žemėlapį iš mūsų CDI Producerio naudojant Qualifier
    @Inject
    @KursuZemelapis
    private Map<String, BigDecimal> valiutuKursai;

    public void pridetiProdukta(Long produktoId) {
        if (produktoId == null) {
            throw new IllegalArgumentException("Produkto ID negali būti null");
        }
        produktuIdKrepselyje.add(produktoId);
    }

    public List<Produktas> getProduktai() {
        return parduotuveService.getProduktaiByIds(produktuIdKrepselyje);
    }

    public int getPrekiuKiekis() {
        return produktuIdKrepselyje.size();
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

        mokejimoServisas.apmoketi(galutineSuma, parinktaValiuta);
        produktuIdKrepselyje.clear();
        return galutineSuma;
    }

    public void isvalyti() {
        produktuIdKrepselyje.clear();
    }

    // Pakeitimas: Visiškai išmesta switch-case logika. Kursai skaitomi dinamiškai iš CDI
    private BigDecimal konvertuotiSuma(BigDecimal sumaEur, String valiuta) {
        if (valiutuKursai == null || !valiutuKursai.containsKey(valiuta)) {
            throw new IllegalArgumentException("Nepalaikoma valiuta: " + valiuta);
        }

        BigDecimal kursas = valiutuKursai.get(valiuta);
        return sumaEur.multiply(kursas).setScale(2, RoundingMode.HALF_UP);
    }
}