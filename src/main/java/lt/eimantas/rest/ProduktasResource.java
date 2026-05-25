package lt.eimantas.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lt.eimantas.entity.Kategorija;
import lt.eimantas.entity.Produktas;
import lt.eimantas.service.ParduotuveService;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("/produktai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProduktasResource {

    @Inject
    private ParduotuveService service;

    @GET
    public List<ProduktasDto> getAll() {
        return service.getVisiProduktai().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public ProduktasDto getById(@PathParam("id") Long id) {
        Produktas p = service.getProduktasById(id);
        if (p == null) {
            throw new NotFoundException("Produktas nerastas");
        }
        return toDto(p);
    }

    @POST
    public Response create(@Valid ProduktasDto dto) {
        Produktas p = new Produktas();
        p.setPavadinimas(dto.getPavadinimas());
        p.setKaina(dto.getKaina());

        if (dto.getKategorijaId() != null) {
            Kategorija kategorija = service.getKategorijaById(dto.getKategorijaId());
            if (kategorija == null) {
                throw new BadRequestException("Nurodyta kategorija neegzistuoja");
            }
            p.setKategorija(kategorija);
        }

        Produktas created = service.sukurtiProdukta(p);
        return Response.created(URI.create("/api/produktai/" + created.getId()))
                .entity(toDto(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    public ProduktasDto update(@PathParam("id") Long id, @Valid ProduktasDto dto) {
        if (dto.getVersion() == null) {
            throw new BadRequestException("PUT uzklausai privalomas version laukas");
        }

        Produktas existing = service.getProduktasById(id);
        if (existing == null) {
            throw new NotFoundException("Produktas nerastas");
        }

        Produktas toUpdate = new Produktas();
        toUpdate.setId(id);
        toUpdate.setVersion(dto.getVersion());
        toUpdate.setPavadinimas(dto.getPavadinimas());
        toUpdate.setKaina(dto.getKaina());
        toUpdate.setSandeliai(existing.getSandeliai());

        if (dto.getKategorijaId() != null) {
            Kategorija kategorija = service.getKategorijaById(dto.getKategorijaId());
            if (kategorija == null) {
                throw new BadRequestException("Nurodyta kategorija neegzistuoja");
            }
            toUpdate.setKategorija(kategorija);
        } else {
            toUpdate.setKategorija(null);
        }

        Produktas updated = service.atnaujintiProdukta(toUpdate);
        return toDto(updated);
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
