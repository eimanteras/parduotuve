package lt.eimantas.rest;

import java.math.BigDecimal;

public class ProduktasDto {
    private Long id;
    private String pavadinimas;
    private BigDecimal kaina;
    private Long kategorijaId;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPavadinimas() {
        return pavadinimas;
    }

    public void setPavadinimas(String pavadinimas) {
        this.pavadinimas = pavadinimas;
    }

    public BigDecimal getKaina() {
        return kaina;
    }

    public void setKaina(BigDecimal kaina) {
        this.kaina = kaina;
    }

    public Long getKategorijaId() {
        return kategorijaId;
    }

    public void setKategorijaId(Long kategorijaId) {
        this.kategorijaId = kategorijaId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

