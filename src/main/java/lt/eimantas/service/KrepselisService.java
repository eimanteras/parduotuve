package lt.eimantas.service;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import lt.eimantas.cdi.KursuZemelapis;
import lt.eimantas.entity.Produktas;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SessionScoped
public class KrepselisService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Long> produktuIdKrepselyje = new ArrayList<>();
    
    // Rezervacijos pabaigos laiko kintamasis
    private LocalDateTime krepselioPabaigosLaikas;

    @Inject
    private ParduotuveService parduotuveService;

    @Inject
    private MokejimoServisas mokejimoServisas;

    @Inject
    @KursuZemelapis
    private Map<String, BigDecimal> valiutuKursai;

    public void pridetiProdukta(Long produktoId) {
        if (produktoId == null) {
            throw new IllegalArgumentException("Produkto ID negali būti null");
        }
        
        // Prieš įdedant naują prekę, patikriname, ar senasis krepšelis jau neišsivalė fone
        tikrintiArLaikasNepasibaige();

        produktuIdKrepselyje.add(produktoId);
        
        // TIMER RESET: Kiekvieną kartą sėkmingai pridėjus prekę, nustatome naujas 15 minučių
        this.krepselioPabaigosLaikas = LocalDateTime.now().plusMinutes(15);
    }

    public String getLikoLaiko() {
        tikrintiArLaikasNepasibaige();

        if (produktuIdKrepselyje.isEmpty() || krepselioPabaigosLaikas == null) {
            return "Krepšelis tuščias, laikmatis neaktyvus.";
        }

        Duration duration = Duration.between(LocalDateTime.now(), krepselioPabaigosLaikas);
        
        if (duration.isNegative() || duration.isZero()) {
            return "Krepšelio laikas pasibaigė!";
        }

        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        return String.format("Iki krepšelio rezervacijos pabaigos liko: %d min. %d sek.", minutes, seconds);
    }

    private void tikrintiArLaikasNepasibaige() {
        if (krepselioPabaigosLaikas != null && LocalDateTime.now().isAfter(krepselioPabaigosLaikas)) {
            produktuIdKrepselyje.clear();
            krepselioPabaigosLaikas = null; 
        }
    }

    public List<Produktas> getProduktai() {
        tikrintiArLaikasNepasibaige();
        return parduotuveService.getProduktaiByIds(produktuIdKrepselyje);
    }

    public int getPrekiuKiekis() {
        tikrintiArLaikasNepasibaige();
        return produktuIdKrepselyje.size();
    }

    public BigDecimal skaiciuotiBendraSumaEur() {
        return getProduktai().stream()
                .map(Produktas::getKaina)
                .filter(kaina -> kaina != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pirkti(String valiuta) {
        tikrintiArLaikasNepasibaige();
        
        if (produktuIdKrepselyje.isEmpty()) {
            throw new IllegalStateException("Krepšelis tuščias arba laikas pasibaigė!");
        }

        String parinktaValiuta = (valiuta == null || valiuta.isBlank())
                ? "EUR"
                : valiuta.trim().toUpperCase(Locale.ROOT);

        BigDecimal sumaEur = skaiciuotiBendraSumaEur();
        BigDecimal galutineSuma = konvertuotiSuma(sumaEur, parinktaValiuta);

        mokejimoServisas.apmoketi(galutineSuma, parinktaValiuta);
        
        // Po sėkmingo pirkimo viską išvalome
        produktuIdKrepselyje.clear();
        this.krepselioPabaigosLaikas = null; 
        
        return galutineSuma;
    }

    public void isvalyti() {
        produktuIdKrepselyje.clear();
        this.krepselioPabaigosLaikas = null;
    }

    private BigDecimal konvertuotiSuma(BigDecimal sumaEur, String valiuta) {
        if (valiutuKursai == null || !valiutuKursai.containsKey(valiuta)) {
            throw new IllegalArgumentException("Nepalaikoma valiuta: " + valiuta);
        }

        BigDecimal kursas = valiutuKursai.get(valiuta);
        return sumaEur.multiply(kursas).setScale(2, RoundingMode.HALF_UP);
    }
}