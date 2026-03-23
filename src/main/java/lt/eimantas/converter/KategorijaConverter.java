package lt.eimantas.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import lt.eimantas.entity.Kategorija;
import lt.eimantas.service.ParduotuveService;

@FacesConverter(value = "kategorijaConverter", managed = true)
public class KategorijaConverter implements Converter<Kategorija> {

    @Inject
    private ParduotuveService service;

    @Override
    public Kategorija getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) return null;
        Long id = Long.parseLong(value);
        return service.getVisiKategorijos().stream()
                .filter(k -> k.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Kategorija kategorija) {
        if (kategorija == null) return "";
        return String.valueOf(kategorija.getId());
    }
}