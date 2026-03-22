package lt.eimantas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "PRODUKTAS")
public class Produktas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;

    @Column(name = "kaina")
    private BigDecimal kaina;

    @ManyToOne
    @JoinColumn(name = "kategorija_id")
    private Kategorija kategorija;

    @ManyToMany
    @JoinTable(
        name = "PRODUKTAS_SANDELIS",
        joinColumns = @JoinColumn(name = "produktas_id"),
        inverseJoinColumns = @JoinColumn(name = "sandelis_id")
    )
    private List<Sandelis> sandeliai;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPavadinimas() { return pavadinimas; }
    public void setPavadinimas(String pavadinimas) { this.pavadinimas = pavadinimas; }

    public BigDecimal getKaina() { return kaina; }
    public void setKaina(BigDecimal kaina) { this.kaina = kaina; }

    public Kategorija getKategorija() { return kategorija; }
    public void setKategorija(Kategorija kategorija) { this.kategorija = kategorija; }

    public List<Sandelis> getSandeliai() { return sandeliai; }
    public void setSandeliai(List<Sandelis> sandeliai) { this.sandeliai = sandeliai; }
}
