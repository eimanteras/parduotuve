package lt.eimantas.dao.jpa;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lt.eimantas.entity.Produktas;

import java.util.Collections;
import java.util.List;

@RequestScoped
public class ProduktasDAO {

    @PersistenceContext(unitName = "ParduotuvePU")
    private EntityManager em;

    public List<Produktas> findAll() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Produktas p " +
                        "LEFT JOIN FETCH p.kategorija " +
                        "LEFT JOIN FETCH p.sandeliai",
                Produktas.class
        ).getResultList();
    }

    @Transactional
    public void save(Produktas p) {
        em.persist(p);
    }

    @Transactional
    public Produktas update(Produktas p) {
        return em.merge(p);
    }

    public void flush() {
        em.flush();
    }

    public void clear() {
        em.clear();
    }

    public Produktas findById(Long id) {
        return em.find(Produktas.class, id);
    }

    public List<Produktas> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return em.createQuery(
                "SELECT DISTINCT p FROM Produktas p " +
                        "LEFT JOIN FETCH p.kategorija " +
                        "LEFT JOIN FETCH p.sandeliai " +
                        "WHERE p.id IN :ids",
                Produktas.class
        ).setParameter("ids", ids)
                .getResultList();
    }
}
