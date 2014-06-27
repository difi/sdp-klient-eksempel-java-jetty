package no.difi.sdp.jersey.resources;

import com.sun.jersey.spi.container.ResourceFilters;
import no.difi.sdp.SDPService;
import no.difi.sdp.jersey.filters.NoCacheResponseFilter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Veldig enkel Resource for å kontrollere brevsending. Bruker GET heller enn POST for å gjøre det trivielt å bruke fra en nettleser.
 */
@Path("/")
@ResourceFilters(NoCacheResponseFilter.class)
public class DigitalPostResource {

    private static final SDPService sdpService;

    /**
     * Må være static slik at vi får singleton av {@link no.digipost.sdp.SDPService}.
     * Blir ellers krøll med kø og status for sending.
     */
    static {
        sdpService = new SDPService();
    }

    @GET
    @Path("start")
    @Produces(TEXT_PLAIN)
    public Response startSending(@DefaultValue("1000") @QueryParam("interval") Integer sendIntervalMs) {
        sdpService.startSending(sendIntervalMs);
        return Response.ok("Ok, sending every " + sendIntervalMs + "ms.").build();
    }

    @GET
    @Path("stop")
    @Produces(TEXT_PLAIN)
    public Response stopSending() {
        sdpService.stopSending();
        return Response.ok("Ok, no longer sending").build();
    }

    @GET
    @Path("receipt")
    @Produces(TEXT_PLAIN)
    public Response receipt() {
        sdpService.pullReceipt();
        return Response.ok("Ok, forced extra polling for receipts").build();
    }

    @GET
    @Path("status")
    @Produces(TEXT_PLAIN)
    public Response status() {
        return Response.ok(sdpService.getStatus()).build();
    }

    @GET
    @Path("queue")
    @Produces(TEXT_PLAIN)
    public Response queue() {
        return Response.ok(sdpService.getQueueStatus()).build();
    }

}
