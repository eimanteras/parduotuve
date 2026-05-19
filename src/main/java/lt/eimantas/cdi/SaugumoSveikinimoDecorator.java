package lt.eimantas.cdi;

//import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

//@Priority(1) // Nurodome prioritetą, kad šis dekoratorius būtų pirmas grandinėje (mažesnis skaičius = aukštesnis prioritetas)
@Decorator // 1. Pasakome CDI, kad tai yra Dekoratorius
public abstract class SaugumoSveikinimoDecorator implements SveikinimoService {

    @Inject
    @Delegate // 2. Įšvirkščiame originalų objektą arba sekantį dekoratorių grandinėje
    private SveikinimoService delegate;

    @Override
    public String suformuoti(String vardas) {
        // 3. Atliekame verslo logikos / saugumo patikrą
        if ("Administratorius".equalsIgnoreCase(vardas)) {
            throw new SecurityException("Kritinė klaida: Prieiga draudžiama! Vartotojas neturi teisės naudoti šio vardo.");
        }
        
        // 4. Jei patikra praėjo, deleguojame darbą toliau
        return delegate.suformuoti(vardas);
    }
}