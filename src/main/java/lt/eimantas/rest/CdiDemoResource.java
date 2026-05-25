package lt.eimantas.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lt.eimantas.cdi.PVMService;
import lt.eimantas.cdi.SkaiciavimoService;

import java.math.BigDecimal;
import java.util.Map;

@Path("/cdi")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CdiDemoResource {

    @Inject
    private PVMService pvmService; 

    @Inject
    private SkaiciavimoService discountService; 

    @GET
    @Path("/test")
    public Response testCdiArchitecture() {
        BigDecimal baseCartPrice = new BigDecimal("100.00");
        
        // 1. Skaičiuojame kainą (PVMDecorator uždės 21% PVM, AuditInterceptor pamatuos greitį)
        BigDecimal priceWithTax = pvmService.calculatePrice(baseCartPrice);
        
        // 2. Paimame nuolaidos koeficientą (Specializes grąžins 0.90)
        double systemDiscount = discountService.getDiscount();
        
        BigDecimal finalPrice = priceWithTax.multiply(BigDecimal.valueOf(systemDiscount))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return Response.ok(Map.of(
            "original_price_eur", baseCartPrice,
            "after_vat_decorator_21_percent", priceWithTax,
            "global_discount_applied", systemDiscount,
            "total_checkout_amount", finalPrice
        )).build();
    }
}