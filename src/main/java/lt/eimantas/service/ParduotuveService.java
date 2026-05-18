package lt.eimantas.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
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
            // Įsivaizduokime, kad dėstytojas liepė ištrinti arba užkomentuoti šią eilutę:
            // produktasDAO.flush(); 
            return atnaujintas;
        } 
        // PATAISYTA: Pridedame jakarta.persistence.RollbackException gaudymą per „Multi-catch“ (|)
        catch (OptimisticLockException | jakarta.persistence.RollbackException ex) {
            
            // Žurnalizuojame, kad matytume, kas tiksliai atėjo
            System.out.println("<<< [TRANSAKCIJA] Pagaute konfliktą! Išimties tipas: " + ex.getClass().getName());
            
            // Po konflikto išvalome persistence context
            produktasDAO.clear();
            
            // Metame tavo suprogramuotą klaidą, kurią REST ExceptionMapperis pavers į HTTP 409
            throw new OptimisticConflictException("Irašas buvo pakeistas kito naudotojo. Atnaujinkite duomenis.", ex);
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
