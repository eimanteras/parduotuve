package lt.eimantas.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lt.eimantas.cdi.MaxPriceCheck;
import lt.eimantas.entity.Kategorija;
import lt.eimantas.entity.Produktas;
import lt.eimantas.service.ParduotuveService;

import java.net.URI;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("/produktai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProduktasResource {

    @Inject
    private ParduotuveService service; // Tavo kintamojo vardas yra 'service'

    @GET
    public Response getAll(@QueryParam("maxKaina") BigDecimal maxKaina) {
        // PATAISYTA: Vietoj parduotuveService dabar naudojame service
        List<Produktas> produktai = service.getVisiProduktai();
        
        // Jei klientas nurodė maxKaina parametrą, išfiltruojame sąrašą
        if (maxKaina != null) {
            produktai = produktai.stream()
                    .filter(p -> p.getKaina() != null && p.getKaina().compareTo(maxKaina) <= 0)
                    .collect(Collectors.toList());
        }
        
        // Konvertuojame likusius produktus į DTO ir grąžiname 200 OK
        List<ProduktasDto> dtoList = produktai.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
                
        return Response.ok(dtoList).build();
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
    @MaxPriceCheck
    public Response create(ProduktasDto dto) {
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
    public ProduktasDto update(@PathParam("id") Long id, ProduktasDto dto) {
        if (dto.getVersion() == null) {
            throw new BadRequestException("PUT uzklausai privalomas version laukas");
        }

        // 1. Surandame TIKRĄJĮ objektą iš DB. Jis automatiškai tampa MANAGED.
        Produktas existing = service.getProduktasById(id);
        if (existing == null) {
            throw new NotFoundException("Produktas nerastas");
        }

        // 2. [KRITIŠKA VIETA] Rankiniu būdu patikriname, ar kliento atsiųsta versija 
        // sutampa su esama DB versija dar PRIEŠ keičiant duomenis.
        if (!Objects.equals(existing.getVersion(), dto.getVersion())) {
            throw new OptimisticConflictException("Irasas buvo pakeistas kito naudotojo. Atnaujinkite duomenis ir bandykite dar karta.");
        }

        // 3. Modifikuojame TIKRĄJĮ managed objektą. Versijos lauko (setVersion) NELIEČIAME!
        existing.setPavadinimas(dto.getPavadinimas());
        existing.setKaina(dto.getKaina());

        if (dto.getKategorijaId() != null) {
            Kategorija kategorija = service.getKategorijaById(dto.getKategorijaId());
            if (kategorija == null) {
                throw new BadRequestException("Nurodyta kategorija neegzistuoja");
            }
            existing.setKategorija(kategorija);
        } else {
            existing.setKategorija(null);
        }

        // 4. Atiduodame atnaujinti. Servise em.merge(existing) tiesiog pratęs darbą,
        // o em.flush() sėkmingai įvykdys automatinį Hibernate tikrinimą.
        Produktas updated = service.atnaujintiProdukta(existing);
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