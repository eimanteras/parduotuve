package lt.eimantas.dao.jpa;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lt.eimantas.entity.Sandelis;

import java.util.List;

@RequestScoped
public class SandelisDAO {

    @PersistenceContext(unitName = "ParduotuvePU")
    private EntityManager em;

    public List<Sandelis> findAll() {
        return em.createQuery("SELECT s FROM Sandelis s", Sandelis.class).getResultList();
    }

    @Transactional
    public void save(Sandelis s) {
        em.persist(s);
    }

    public Sandelis findById(Long id) {
        return em.find(Sandelis.class, id);
    }
}

