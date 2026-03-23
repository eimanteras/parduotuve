package lt.eimantas.dao.mybatis;

public class SandelisModel {
    private Long id;
    private String pavadinimas;
    private String adresas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPavadinimas() { return pavadinimas; }
    public void setPavadinimas(String pavadinimas) { this.pavadinimas = pavadinimas; }

    public String getAdresas() { return adresas; }
    public void setAdresas(String adresas) { this.adresas = adresas; }
}

