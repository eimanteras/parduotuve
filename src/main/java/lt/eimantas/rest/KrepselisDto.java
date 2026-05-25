package lt.eimantas.rest;

import java.math.BigDecimal;
import java.util.List;

public class KrepselisDto {

    private List<ProduktasDto> produktai;
    private BigDecimal bendraSumaEur;

    public List<ProduktasDto> getProduktai() {
        return produktai;
    }

    public void setProduktai(List<ProduktasDto> produktai) {
        this.produktai = produktai;
    }

    public BigDecimal getBendraSumaEur() {
        return bendraSumaEur;
    }

    public void setBendraSumaEur(BigDecimal bendraSumaEur) {
        this.bendraSumaEur = bendraSumaEur;
    }
}