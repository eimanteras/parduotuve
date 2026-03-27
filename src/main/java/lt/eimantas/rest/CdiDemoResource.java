package lt.eimantas.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lt.eimantas.cdi.PristatymoService;
import lt.eimantas.cdi.SkaiciavimoService;
import lt.eimantas.cdi.SveikinimoService;

import java.util.Map;

@Path("/cdi")
@Produces(MediaType.APPLICATION_JSON)
public class CdiDemoResource {

    @Inject
    private PristatymoService pristatymoService;

    @Inject
    private SkaiciavimoService skaiciavimoService;

    @Inject
    private SveikinimoService sveikinimoService;

    @GET
    public Map<String, String> demo() {
        return Map.of(
                "alternative", pristatymoService.tipas(),
                "specialization", skaiciavimoService.versija(),
                "interceptorDecorator", sveikinimoService.suformuoti("Eimantas")
        );
    }
}

