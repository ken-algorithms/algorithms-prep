package com.prep.cleancode.before;

import static com.prep.cleancode.before.ServiceName.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * PHIEN BAN "BEFORE" - CO Y VIET XAU. DUNG SUA FILE NAY.
 * ============================================================================
 *
 * <p>Day la ban distilled (da an danh, bo ten vendor) cua chinh class
 * {@code HealthCheckContextStrategy} trong project RCI cua ban:
 * {@code rci-backend/rci-gateway/src/main/java/com/rci/gateway/app/strategy/}
 *
 * <p>Neu SonarLint trong IDE sang den do khap file nay -> DUNG MUC DICH. Nhiem vu cua ban la doc
 * README.md, tim ra 12 van de, roi so voi package {@code after}.
 *
 * <p>Van de lon nhat KHONG phai loi cu phap. No la loi THIET KE: day duoc goi la "Strategy
 * pattern" nhung lai co mot switch o giua. Strategy ton tai chinh xac de XOA cai switch do. Con
 * switch = con phai sua class nay moi lan them service -> vi pham Open/Closed.
 */
public class ServiceHealthAggregatorBefore {

    // [1] Field injection (mo phong @Autowired field). Field khong final, public/package-private
    //     -> khong the tao object o trang thai hop le bang constructor, test phai set tay tung field.
    HealthCheckStrategy authHealthCheckStrategy;
    HealthCheckStrategy incidentHubHealthCheckStrategy;

    // [2] 6 dependency duoi day duoc khai bao nhung KHONG BAO GIO duoc dung trong switch ben duoi.
    //     Trong code RCI that: 14 strategy duoc @Autowired, switch chi xu ly 2.
    HealthCheckStrategy paymentBridgeHealthCheckStrategy;
    HealthCheckStrategy erpBridgeHealthCheckStrategy;
    HealthCheckStrategy accountHealthCheckStrategy;
    HealthCheckStrategy batchHealthCheckStrategy;
    HealthCheckStrategy coreHealthCheckStrategy;
    HealthCheckStrategy reportingHealthCheckStrategy;

    // [3] Magic string lap lai, khong nhom lai theo nghia.
    private static final String STATUS = "status";
    private static final String VERSION = "version";

    public Map<String, Object> checkHealth() {
        var health = doHealthCheck();
        // [4] Tra ve HashMap mutable ra ngoai -> caller sua duoc noi dung ben trong.
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("SHARED", health);
        return wrapper;
    }

    protected Map<String, Object> doHealthCheck() {
        List<ServiceName> services = List.of(AUTHENTICATION, INCIDENT_HUB);

        // [5] Collectors.toMap khong co merge function -> trung key la nem IllegalStateException.
        Map<ServiceName, CompletableFuture<Health>> jobMap =
                services.stream().collect(Collectors.toMap(s -> s, this::getDoCheckFuture));

        // [6] Bien chet: tinh xong roi khong dung. Con lam cho moi future bi cho DOI HAI LAN
        //     (join o day, roi .get() lai trong getSafe ben duoi).
        List<Health> doneJobs = jobMap.values().stream().map(CompletableFuture::join).toList();

        Map<String, Object> result = new HashMap<>();
        result.put(AUTHENTICATION.getId(), buildResponse(getSafe(jobMap.get(AUTHENTICATION))));
        result.put(INCIDENT_HUB.getId(), buildResponse(getSafe(jobMap.get(INCIDENT_HUB))));
        return result;
    }

    /**
     * [7] LOI THIET KE TRUNG TAM: switch trong Strategy pattern.
     *
     * <p>Them 1 service moi = phai sua method nay + them field + them @Qualifier. Do la
     * Open/Closed violation. Ngoai ra `default` nem exception luc RUNTIME, trong khi neu thiet ke
     * dung thi compiler/DI container se phat hien thieu ngay luc khoi dong.
     */
    private CompletableFuture<Health> getDoCheckFuture(ServiceName service) {
        switch (service) {
            case AUTHENTICATION:
                return authHealthCheckStrategy.doCheck();
            case INCIDENT_HUB:
                return incidentHubHealthCheckStrategy.doCheck();
            default:
                throw new UnsupportedOperationException(
                        "Health check for service " + service + " is not implemented.");
        }
    }

    /**
     * [8] Nuot exception va tra ve null. Ba loi cung luc:
     * bat Exception chung chung; khong log gi ca; dung null lam gia tri bao loi.
     * Moi caller phia sau bat buoc phai null-check, va khi quen thi thanh NPE o cho khac.
     *
     * <p>[9] Khong co TIMEOUT. `future.get()` khong tham so se cho VO HAN. Neu 1 downstream treo,
     * ca health check treo theo -> chinh cai health check tro thanh su co.
     */
    private Health getSafe(CompletableFuture<Health> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * [10] Varargs theo VI TRI: caller phai nho dung thu tu, compiler khong giup. Truyen thieu
     * phan tu -> ArrayIndexOutOfBounds luc runtime. Method nay con khong duoc ai goi (dead code).
     */
    @SuppressWarnings("unused")
    private Map<String, Object> buildThirdPartyResponse(Health... health) {
        Health core = health[0];
        Health communication = health[1];
        Health origination = health[2];
        var out = new HashMap<String, Object>();
        out.put("core", core.getDetails().get("vendorA"));
        out.put("communication", communication.getDetails().get("vendorB"));
        out.put("origination", origination.getDetails().get("vendorC"));
        return out;
    }

    private Map<String, Object> buildResponse(Health health) {
        var resp = new HashMap<String, Object>();
        if (health == null) {
            // [11] Thong diep log SAI va vo nghia, lai bi copy-paste o nhieu cho.
            //      Doc log nay khi production chay se khong biet service nao, vi sao.
            System.err.println("Can not update properties.");
        } else {
            resp.put(STATUS, health.getStatus());
            resp.put(VERSION, String.valueOf(health.getDetails().get("version")));

            var metric = browse(resp, "metric");
            if (metric != null) {
                var memory = browse(metric, "memory");
                double used = (double) memory.get("used");
                double max = (double) memory.get("max");
                memory.put("percent", used * 100 / max);
                // [12] Code comment-out bo lai trong repo (Sonar java:S125).
                //      Khong ai dam xoa vi khong biet con can khong -> no o day mai mai.
                // browse(metric, THREAD).remove("states");
                // browse(metric, THREAD).put(CPU, nanoToMili((long) browse(metric,
                // THREAD).get(CPU)));
            }
        }
        return resp;
    }

    /** [13] Unchecked cast an trong helper - de nem ClassCastException o cho khong lien quan. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> browse(Map<String, Object> map, String fieldName) {
        return (Map<String, Object>) map.get(fieldName);
    }

    /** [14] Tra ve list mutable, con dung ArrayList khi khong can thay doi. */
    public List<String> knownServiceIds() {
        var ids = new ArrayList<String>();
        ids.add(AUTHENTICATION.getId());
        ids.add(INCIDENT_HUB.getId());
        return ids;
    }

    /**
     * ========================================================================
     * FIXTURE DAY HOC - ban sao cua doHealthCheck() DA XOA dong "bien chet".
     * ========================================================================
     *
     * <p>Ton tai chi de test chung minh mot dieu phan truc giac:
     *
     * <p>Trong {@link #doHealthCheck()}, dong bien chet {@code doneJobs} goi {@code join()} TRUOC
     * {@code getSafe()}. Ma {@code join()} tren mot future FAILED thi NEM CompletionException.
     * Ket qua: khi downstream loi, ca method no tung ra ngoai -> endpoint tra 500. Va tro treu la
     * {@code getSafe()} - doan duy nhat duoc viet ra DE xu ly loi - khong bao gio chay toi.
     *
     * <p>Neu ban "don dep vo hai" bang cach xoa dong bien chet do (dieu ma bat ky ai review cung
     * se de nghi), hanh vi DOI HAN: khong con nem nua, thay vao do getSafe() nuot loi va tra null,
     * roi response thanh RONG mot cach im lang.
     *
     * <p>Hai bug dang CHE LAP NHAU. Do la ly do vi sao khong duoc refactor code khong co test:
     * "xoa dead code" nghe nhu zero-risk, nhung o day no bien loi-on-ao thanh loi-im-lang, tuc la
     * bien mot su co de thay thanh mot su co khong ai thay trong 3 thang.
     */
    protected Map<String, Object> doHealthCheckWithoutDeadVariable() {
        List<ServiceName> services = List.of(AUTHENTICATION, INCIDENT_HUB);
        Map<ServiceName, CompletableFuture<Health>> jobMap =
                services.stream().collect(Collectors.toMap(s -> s, this::getDoCheckFuture));

        // (dong `doneJobs` da duoc xoa o day)

        Map<String, Object> result = new HashMap<>();
        result.put(AUTHENTICATION.getId(), buildResponse(getSafe(jobMap.get(AUTHENTICATION))));
        result.put(INCIDENT_HUB.getId(), buildResponse(getSafe(jobMap.get(INCIDENT_HUB))));
        return result;
    }
}
