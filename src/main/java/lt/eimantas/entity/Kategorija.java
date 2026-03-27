package lt.eimantas.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "KATEGORIJA")

public class Kategorija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pavadinimas", length = 100, nullable = false)
    private String pavadinimas;

    @OneToMany(mappedBy = "kategorija", fetch = FetchType.LAZY)
    private List<Produktas> produktai;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPavadinimas() { return pavadinimas; }
    public void setPavadinimas(String pavadinimas) { this.pavadinimas = pavadinimas; }

    public List<Produktas> getProduktai() { return produktai; }
    public void setProduktai(List<Produktas> produktai) { this.produktai = produktai; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kategorija that = (Kategorija) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
