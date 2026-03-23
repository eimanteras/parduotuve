package lt.eimantas.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lt.eimantas.dao.mybatis.ProduktasMapper;
import lt.eimantas.dao.mybatis.ProduktasModel;
import lt.eimantas.dao.mybatis.SandelisMapper;
import lt.eimantas.entity.Kategorija;
import lt.eimantas.entity.Produktas;
import lt.eimantas.entity.Sandelis;
import lt.eimantas.service.ParduotuveService;

import java.util.List;

@Named
@RequestScoped
public class ParduotuveBean {

    @Inject
    private ParduotuveService service;

    @Inject
    private ProduktasMapper produktasMapper;

    @Inject
    private SandelisMapper sandelisMapper;

    // Data binding objektai
    private Produktas naujasProduktas = new Produktas();
    private Kategorija naujaKategorija = new Kategorija();
    private Sandelis naujasSandelis = new Sandelis();

    public String issaugotiProdukta() {
        service.issaugotiProdukta(naujasProduktas);
        naujasProduktas = new Produktas();
        return "produktai?faces-redirect=true";
    }

    public String issaugotiKategorija() {
        service.issaugotiKategorija(naujaKategorija);
        naujaKategorija = new Kategorija();
        return "kategorijos?faces-redirect=true";
    }

    public List<Produktas> getVisiProduktai() {
        return service.getVisiProduktai();
    }

    public List<Kategorija> getVisiKategorijos() {
        return service.getVisiKategorijos();
    }

    // MyBatis produktai
    public List<ProduktasModel> getMyBatisProduktai() {
        return produktasMapper.findAll();
    }

    public List<Sandelis> getVisiSandeliai() {
        return service.getVisiSandeliai();
    }

    public String issaugotiSandelį() {
        service.issaugotiSandelį(naujasSandelis);
        naujasSandelis = new Sandelis();
        return "sandeliai?faces-redirect=true";
    }

    public Produktas getNaujasProduktas() { return naujasProduktas; }
    public void setNaujasProduktas(Produktas naujasProduktas) { this.naujasProduktas = naujasProduktas; }

    public Kategorija getNaujaKategorija() { return naujaKategorija; }
    public void setNaujaKategorija(Kategorija naujaKategorija) { this.naujaKategorija = naujaKategorija; }

    public Sandelis getNaujasSandelis() { return naujasSandelis; }
    public void setNaujasSandelis(Sandelis naujasSandelis) { this.naujasSandelis = naujasSandelis; }
}
