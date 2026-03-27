package lt.eimantas.dao.jpa;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lt.eimantas.entity.Kategorija;

import java.util.List;

@RequestScoped
public class KategorijaDAO {

    @PersistenceContext(unitName = "ParduotuvePU")
    private EntityManager em;

    public List<Kategorija> findAll() {
        return em.createQuery(
                "SELECT DISTINCT k FROM Kategorija k LEFT JOIN FETCH k.produktai",
                Kategorija.class
        ).getResultList();
    }

    public Kategorija findById(Long id) {
        return em.find(Kategorija.class, id);
    }

    @Transactional
    public void save(Kategorija k) {
        em.persist(k);
    }
}
