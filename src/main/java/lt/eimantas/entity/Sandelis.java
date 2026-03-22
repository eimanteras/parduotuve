package lt.eimantas.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "SANDELIS")
public class Sandelis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;

    @Column(name = "adresas", length = 200)
    private String adresas;

    @ManyToMany(mappedBy = "sandeliai", fetch = FetchType.LAZY)
    private List<Produktas> produktai;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPavadinimas() { return pavadinimas; }
    public void setPavadinimas(String pavadinimas) { this.pavadinimas = pavadinimas; }

    public String getAdresas() { return adresas; }
    public void setAdresas(String adresas) { this.adresas = adresas; }

    public List<Produktas> getProduktai() { return produktai; }
    public void setProduktai(List<Produktas> produktai) { this.produktai = produktai; }
}
