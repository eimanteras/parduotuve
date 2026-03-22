package lt.eimantas.dao.mybatis;

import java.math.BigDecimal;

public class ProduktasModel {
    private Long id;
    private String pavadinimas;
    private BigDecimal kaina;
    private Long kategorijaId;
    private String kategorijaPavadinimas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPavadinimas() { return pavadinimas; }
    public void setPavadinimas(String pavadinimas) { this.pavadinimas = pavadinimas; }

    public BigDecimal getKaina() { return kaina; }
    public void setKaina(BigDecimal kaina) { this.kaina = kaina; }

    public Long getKategorijaId() { return kategorijaId; }
    public void setKategorijaId(Long kategorijaId) { this.kategorijaId = kategorijaId; }

    public String getKategorijaPavadinimas() { return kategorijaPavadinimas; }
    public void setKategorijaPavadinimas(String kategorijaPavadinimas) { this.kategorijaPavadinimas = kategorijaPavadinimas; }
}
