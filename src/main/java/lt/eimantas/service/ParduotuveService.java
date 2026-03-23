package lt.eimantas.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import lt.eimantas.dao.jpa.KategorijaDAO;
import lt.eimantas.dao.jpa.ProduktasDAO;
import lt.eimantas.dao.jpa.SandelisDAO;
import lt.eimantas.entity.Kategorija;
import lt.eimantas.entity.Produktas;
import lt.eimantas.entity.Sandelis;

import java.util.List;

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

    public void issaugotiKategorija(Kategorija k) {
        kategorijaDAO.save(k);
    }

    public List<Sandelis> getVisiSandeliai() {
        return sandelisDAO.findAll();
    }

    public void issaugotiSandelį(Sandelis s) {
        sandelisDAO.save(s);
    }
}
