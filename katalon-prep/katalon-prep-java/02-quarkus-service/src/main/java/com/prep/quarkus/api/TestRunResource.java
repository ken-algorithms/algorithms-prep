package com.prep.quarkus.api;

import com.prep.quarkus.app.TestRunService;
import com.prep.quarkus.domain.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Tang HTTP: chi lam 3 viec - doc request, goi service, map sang HTTP status.
 * KHONG co logic nghiep vu o day.
 */
@Path("/api/runs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestRunResource {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final TestRunService service;

    public TestRunResource(TestRunService service) {
        this.service = service;
    }

    @POST
    public Response create(
            @HeaderParam(TENANT_HEADER) String tenantId, @Valid Dtos.CreateRunRequest request) {
        var created = service.createRun(requireTenant(tenantId), request);
        // 201 + Location header - dung chuan REST, khong tra 200 cho viec tao moi.
        return Response.created(java.net.URI.create("/api/runs/" + created.id()))
                .entity(created)
                .build();
    }

    @POST
    @Path("/{runId}/results")
    public Response submitResults(
            @HeaderParam(TENANT_HEADER) String tenantId,
            @PathParam("runId") Long runId,
            @Valid Dtos.SubmitResultsRequest request) {
        int accepted = service.submitResults(requireTenant(tenantId), runId, request);
        return Response.accepted(Map.of("accepted", accepted)).build();
    }

    /**
     * BAY DA GAP THAT khi chay test: class nay co @Consumes(APPLICATION_JSON) o muc class, nen
     * endpoint KHONG CO BODY nay cung doi header Content-Type -> client goi khong kem header thi
     * nhan 415 (hoac 500 neu mapper sai, xem ApiExceptionMappers).
     *
     * <p>Sua bang @Consumes(WILDCARD): mot endpoint khong nhan body thi khong duoc doi content-type.
     * Day la loi API design rat de mac khi dat @Consumes o muc class cho tien.
     */
    @POST
    @Path("/{runId}/complete")
    @Consumes(MediaType.WILDCARD)
    public Dtos.RunResponse complete(
            @HeaderParam(TENANT_HEADER) String tenantId, @PathParam("runId") Long runId) {
        return service.completeRun(requireTenant(tenantId), runId);
    }

    @GET
    @Path("/{runId}/summary")
    public Dtos.RunSummary summary(
            @HeaderParam(TENANT_HEADER) String tenantId, @PathParam("runId") Long runId) {
        return service.summary(requireTenant(tenantId), runId);
    }

    @GET
    public List<Dtos.RunResponse> list(
            @HeaderParam(TENANT_HEADER) String tenantId,
            @QueryParam("status") RunStatus status,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            // Gioi han pageSize: khong de client keo ca bang ve bang ?size=1000000
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        return service.list(requireTenant(tenantId), status, page, size);
    }

    private static String requireTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException(TENANT_HEADER + " header is required");
        }
        return tenantId;
    }
}
