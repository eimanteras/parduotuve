package lt.eimantas.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lt.eimantas.entity.Produktas;
import lt.eimantas.service.KrepselisService;
import lt.eimantas.service.ParduotuveService;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/krepselis")
@Produces(MediaType.APPLICATION_JSON)
public class KrepselisResource {

    @Inject
    private KrepselisService krepselisService;

    @Inject
    private ParduotuveService parduotuveService;

    @GET
    public KrepselisDto getKrepselis() {
        KrepselisDto dto = new KrepselisDto();
        dto.setProduktai(krepselisService.getProduktai().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        dto.setBendraSumaEur(krepselisService.skaiciuotiBendraSumaEur());
        return dto;
    }

    @POST
    @Path("/prideti/{id}")
    public Response prideti(@PathParam("id") Long id) {
        Produktas produktas = parduotuveService.getProduktasById(id);
        if (produktas == null) {
            throw new NotFoundException("Produktas nerastas");
        }

        krepselisService.pridetiProdukta(id);
        return Response.ok(Map.of(
                "message", produktas.getPavadinimas() + " pridėtas į krepšelį",
                "prekiuKiekis", krepselisService.getProduktai().size()
        )).build();
    }

    @POST
    @Path("/pirkti")
    public Response pirkti(@HeaderParam("X-Currency") String valiuta) {
        try {
            BigDecimal galutineSuma = krepselisService.pirkti(valiuta);
            String atsakymoValiuta = (valiuta == null || valiuta.isBlank())
                    ? "EUR"
                    : valiuta.trim().toUpperCase(Locale.ROOT);

            return Response.ok(Map.of(
                    "message", "Pirkimas sėkmingai atliktas",
                    "suma", galutineSuma,
                    "valiuta", atsakymoValiuta
            )).build();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    @DELETE
    public Response isvalyti() {
        krepselisService.isvalyti();
        return Response.noContent().build();
    }

    private ProduktasDto toDto(Produktas p) {
        ProduktasDto dto = new ProduktasDto();
        dto.setId(p.getId());
        dto.setPavadinimas(p.getPavadinimas());
        dto.setKaina(p.getKaina());
        dto.setVersion(p.getVersion());
        dto.setKategorijaId(p.getKategorija() != null ? p.getKategorija().getId() : null);
        return dto;
    }
}