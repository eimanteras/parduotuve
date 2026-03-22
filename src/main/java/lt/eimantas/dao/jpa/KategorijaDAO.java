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
        return em.createQuery("SELECT k FROM Kategorija k", Kategorija.class).getResultList();
    }

    @Transactional
    public void save(Kategorija k) {
        em.persist(k);
    }
}
