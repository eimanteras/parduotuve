package lt.eimantas.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lt.eimantas.dao.jpa.KategorijaDAO;
import lt.eimantas.dao.jpa.ProduktasDAO;
import lt.eimantas.dao.jpa.SandelisDAO;
import lt.eimantas.entity.Kategorija;
import lt.eimantas.entity.Produktas;
import lt.eimantas.entity.Sandelis;
import lt.eimantas.rest.OptimisticConflictException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequestScoped
public class ParduotuveService {

    @Inject
    private ProduktasDAO produktasDAO;

    @Inject
    private KategorijaDAO kategorijaDAO;

    @Inject
    private SandelisDAO sandelisDAO;

    public List<Produktas> getVisiProduktai() {
        return produktasDAO.findAll();
    }

    public List<Kategorija> getVisiKategorijos() {
        return kategorijaDAO.findAll();
    }

    public void issaugotiProdukta(Produktas p) {
        produktasDAO.save(p);
    }

    public Produktas getProduktasById(Long id) {
        return produktasDAO.findById(id);
    }

    public List<Produktas> getProduktaiByIds(List<Long> ids) {
        return produktasDAO.findByIds(ids);
    }

    @Transactional
    public Produktas sukurtiProdukta(Produktas p) {
        produktasDAO.save(p);
        produktasDAO.flush();
        return p;
    }

    @Transactional
    public Produktas atnaujintiProdukta(Produktas p) {
        try {
            Produktas atnaujintas = produktasDAO.update(p);
            produktasDAO.flush();
            return atnaujintas;
        } catch (OptimisticLockException ex) {
            // Po OptimisticLockException esamas persistence context laikomas nepatikimu.
            produktasDAO.clear();
            throw new OptimisticConflictException("Irasas buvo pakeistas kito naudotojo. Atnaujinkite duomenis ir bandykite dar karta.", ex);
        }
    }

    public Kategorija getKategorijaById(Long id) {
        return kategorijaDAO.findById(id);
    }

    public List<Sandelis> getSandeliaiByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return ids.stream()
                .map(sandelisDAO::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void issaugotiKategorija(Kategorija k) {
        kategorijaDAO.save(k);
    }

    public List<Sandelis> getVisiSandeliai() {
        return sandelisDAO.findAll();
    }

    public void issaugotiSandelis(Sandelis s) {
        sandelisDAO.save(s);
    }
}
