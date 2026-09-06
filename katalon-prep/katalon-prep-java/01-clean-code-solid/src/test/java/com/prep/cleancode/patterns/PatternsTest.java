package com.prep.cleancode.patterns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.cleancode.patterns.engine.EngineSelection;
import com.prep.cleancode.patterns.engine.EngineSelection.Capability;
import com.prep.cleancode.patterns.engine.EngineSelection.Platform;
import com.prep.cleancode.patterns.events.TestEventBus;
import com.prep.cleancode.patterns.events.TestEventBus.TestEvent;
import com.prep.cleancode.patterns.execution.Executors;
import com.prep.cleancode.patterns.execution.Executors.StepResult;
import com.prep.cleancode.patterns.execution.Executors.TestStep;
import com.prep.cleancode.patterns.lifecycle.TestLifecycle;
import com.prep.cleancode.patterns.lifecycle.TestLifecycle.TestCase;
import com.prep.cleancode.patterns.locator.Locators;
import com.prep.cleancode.patterns.locator.Locators.Element;
import com.prep.cleancode.patterns.locator.Locators.Locator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PatternsTest {

    private static final String SUBMIT = "submit";

    // ==================================================================
    // CHAIN OF RESPONSIBILITY — self-healing locator
    // ==================================================================

    @Nested
    @DisplayName("Chain of Responsibility - self-healing locator")
    class SelfHealing {

        /** Page fake: chi match cac locator duoc liet ke. */
        private static Locators.Page pageMatching(Locator... matching) {
            var allowed = List.of(matching);
            return locator -> allowed.contains(locator)
                    ? Optional.of(new Element("button", "Submit"))
                    : Optional.empty();
        }

        private static List<Locator> defaultChain() {
            return List.of(
                    new Locator.TestId(SUBMIT),
                    new Locator.Role("button", "Submit"),
                    new Locator.Text("Submit"),
                    new Locator.Css("#submit-btn"),
                    new Locator.PositionalXPath("/html/body/div[3]/form/button[1]"));
        }

        @Test
        @DisplayName("chuoi tu sap xep theo do ben - nguoi viet test khong phai nho thu tu")
        void chainIsSortedByStability() {
            // CO Y truyen nguoc: mong manh nhat truoc.
            var resolver = new Locators.SelfHealingLocator(
                    List.of(
                            new Locator.PositionalXPath("/html/body/button"),
                            new Locator.Css("#x"),
                            new Locator.TestId(SUBMIT)),
                    null);

            assertEquals(
                    List.of(100, 40, 10),
                    resolver.chain().stream().map(Locator::stability).toList());
        }

        @Test
        @DisplayName("locator uu tien match -> KHONG phai healed, confidence toi da")
        void preferredLocatorMeansNoHealing() {
            var telemetry = new Locators.InMemoryTelemetry();
            var resolver = new Locators.SelfHealingLocator(defaultChain(), telemetry);

            var resolution = resolver.resolve(pageMatching(new Locator.TestId(SUBMIT)));

            assertFalse(resolution.healed());
            assertEquals(0, resolution.attemptIndex());
            assertEquals(100, resolution.confidence());
            assertTrue(resolution.trustworthyEnoughToAutoUpdate());
            assertTrue(resolution.triedAndFailed().isEmpty());
            assertEquals(0, telemetry.healCounts().size(), "khong heal thi khong ghi telemetry");
        }

        @Test
        @DisplayName("data-testid bien mat -> CHUA duoc bang role, ghi telemetry, confidence giam")
        void healsToNextStrategyAndRecordsTelemetry() {
            var telemetry = new Locators.InMemoryTelemetry();
            var resolver = new Locators.SelfHealingLocator(defaultChain(), telemetry);

            // Chi con role match (dev xoa data-testid).
            var resolution = resolver.resolve(pageMatching(new Locator.Role("button", "Submit")));

            assertTrue(resolution.healed());
            assertEquals(1, resolution.attemptIndex());
            // role stability 80 - penalty 15 = 65
            assertEquals(65, resolution.confidence());
            assertEquals(1, resolution.triedAndFailed().size());

            // Telemetry ghi ro "tu locator nao sang locator nao" -> du de de xuat cap nhat test.
            assertEquals(
                    Map_of("[data-testid='submit'] -> role=button[name='Submit']", 1),
                    telemetry.healCounts());
        }

        @Test
        @DisplayName("phai xuong sau trong chuoi -> confidence duoi nguong -> KHONG duoc auto-update")
        void deepHealingIsNotTrustworthyEnoughToAutoUpdate() {
            var resolver = new Locators.SelfHealingLocator(defaultChain(), null);

            // Chi con XPath theo vi tri match - dau hieu DOM da doi rat nhieu.
            var resolution = resolver.resolve(
                    pageMatching(new Locator.PositionalXPath("/html/body/div[3]/form/button[1]")));

            assertTrue(resolution.healed());
            assertEquals(4, resolution.attemptIndex());
            // stability 10 - penalty 60 -> ket qua bi ep ve 0
            assertEquals(0, resolution.confidence());

            // Diem quan trong: van TIM THAY element (test chay tiep duoc), nhung he thong
            // KHONG duoc tu y sua test suite cua khach. Phai cho nguoi xac nhan.
            assertFalse(resolution.trustworthyEnoughToAutoUpdate());
        }

        @Test
        @DisplayName("khong locator nao match -> nem, kem DANH SACH da thu de debug")
        void unresolvedThrowsWithDiagnostics() {
            var telemetry = new Locators.InMemoryTelemetry();
            var resolver = new Locators.SelfHealingLocator(defaultChain(), telemetry);

            var ex = assertThrows(
                    Locators.ElementNotFoundException.class, () -> resolver.resolve(pageMatching()));

            assertEquals(5, ex.tried().size(), "phai bao cao het cac locator da thu");
            assertTrue(ex.getMessage().contains("data-testid"), ex.getMessage());
            assertEquals(1, telemetry.unresolvedCount());
        }

        @Test
        @DisplayName("mot locator lam Page NEM -> coi nhu khong thay, chuoi VAN di tiep")
        void throwingPageDoesNotBreakTheChain() {
            var resolver = new Locators.SelfHealingLocator(defaultChain(), null);

            Locators.Page flakyPage = locator -> {
                if (locator instanceof Locator.TestId) {
                    throw new IllegalStateException("CDP connection lost");
                }
                return locator instanceof Locator.Role
                        ? Optional.of(new Element("button", "Submit"))
                        : Optional.empty();
            };

            var resolution = resolver.resolve(flakyPage);
            assertTrue(resolution.healed());
            assertEquals(1, resolution.attemptIndex());
        }

        @Test
        @DisplayName("Page tra null (vi pham hop dong) -> khong NPE")
        void nullFromPageIsTreatedAsNotFound() {
            var resolver = new Locators.SelfHealingLocator(
                    List.of(new Locator.TestId(SUBMIT), new Locator.Role("button", "Submit")), null);

            Locators.Page misbehaving = locator ->
                    locator instanceof Locator.Role ? Optional.of(new Element("button", "s")) : null;

            var resolution = resolver.resolve(misbehaving);
            assertTrue(resolution.healed());
        }

        @Test
        @DisplayName("chuoi rong la loi cau hinh -> chan tai constructor")
        void emptyChainRejected() {
            assertThrows(
                    IllegalArgumentException.class, () -> new Locators.SelfHealingLocator(List.of(), null));
        }

        private static java.util.Map<String, Integer> Map_of(String k, int v) {
            return java.util.Map.of(k, v);
        }
    }

    // ==================================================================
    // DECORATOR
    // ==================================================================

    @Nested
    @DisplayName("Decorator - retry / timing / screenshot")
    class Decorators {

        /** Step fail N lan dau roi pass — mo phong test flaky. */
        private static TestStep flakyStep(String name, int failuresBeforePass) {
            var calls = new AtomicInteger();
            return new TestStep(name, () -> calls.incrementAndGet() > failuresBeforePass);
        }

        @Test
        @DisplayName("retry: pass sau 2 lan fail -> passed=true nhung attempts=3 (tuc la FLAKY)")
        void retryReportsAttemptsSoFlakinessIsVisible() {
            var executor = new Executors.RetryingExecutor(new Executors.PlainExecutor(), 3);

            StepResult result = executor.execute(flakyStep("login", 2));

            assertTrue(result.passed());
            assertEquals(3, result.attempts());
            // Day la diem then chot: attempts > 1 && passed  =>  FLAKY, khong phai PASS.
            // Che con so nay di la cach test suite muc dan ma khong ai biet.
            assertTrue(result.attempts() > 1, "phai bao cao la flaky, khong duoc bao pass tron");
        }

        @Test
        @DisplayName("retry het luot van fail -> passed=false, giu nguyen reason cuoi")
        void retryExhaustedKeepsFailure() {
            var executor = new Executors.RetryingExecutor(new Executors.PlainExecutor(), 3);

            StepResult result = executor.execute(new TestStep("checkout", () -> false));

            assertFalse(result.passed());
            assertEquals(3, result.attempts());
            assertEquals("assertion failed", result.failureReason());
        }

        @Test
        @DisplayName("step nem exception -> thanh StepResult.fail co ten class, khong tung ra ngoai")
        void exceptionBecomesFailureNotCrash() {
            StepResult result = new Executors.PlainExecutor()
                    .execute(new TestStep("boom", () -> {
                        throw new IllegalStateException("driver died");
                    }));

            assertFalse(result.passed());
            assertTrue(result.failureReason().contains("IllegalStateException"));
            assertTrue(result.failureReason().contains("driver died"));
        }

        @Test
        @DisplayName("THU TU DECORATOR: retry(timing) do TUNG attempt, timing(retry) do TONG")
        void decoratorOrderChangesMeaning() {
            // retry(timing(plain)): TimingExecutor o trong -> moi attempt duoc do rieng,
            // ket qua tra ve la duration cua attempt CUOI.
            var retryOutside = new Executors.RetryingExecutor(
                    new Executors.TimingExecutor(new Executors.PlainExecutor()), 3);

            // timing(retry(plain)): TimingExecutor o ngoai -> do tong ca 3 attempt.
            var timingOutside = new Executors.TimingExecutor(
                    new Executors.RetryingExecutor(new Executors.PlainExecutor(), 3));

            var slowFlaky = new java.util.function.Supplier<TestStep>() {
                @Override
                public TestStep get() {
                    var calls = new AtomicInteger();
                    return new TestStep("slow", () -> {
                        sleep(60);
                        return calls.incrementAndGet() > 2; // fail 2 lan dau
                    });
                }
            };

            StepResult perAttempt = retryOutside.execute(slowFlaky.get());
            StepResult total = timingOutside.execute(slowFlaky.get());

            assertTrue(perAttempt.passed());
            assertTrue(total.passed());
            assertEquals(3, perAttempt.attempts());
            assertEquals(3, total.attempts());

            // 3 attempt x ~60ms. Do tung attempt ~60ms; do tong ~180ms.
            // Neu dat sai thu tu, dashboard p95 se noi doi ve trai nghiem nguoi dung.
            assertTrue(
                    total.duration().toMillis() > perAttempt.duration().toMillis(),
                    "tong=%d ms phai lon hon mot attempt=%d ms"
                            .formatted(total.duration().toMillis(), perAttempt.duration().toMillis()));
        }

        @Test
        @DisplayName("screenshot chi chup khi FAIL, va loi chup khong che mat loi goc")
        void screenshotOnlyOnFailureAndNeverMasksTheRealError() {
            var passing = new Executors.ScreenshotOnFailureExecutor(
                    new Executors.PlainExecutor(), () -> "shot.png");
            assertTrue(passing.execute(new TestStep("ok", () -> true)).artifacts().isEmpty());

            var failing = new Executors.ScreenshotOnFailureExecutor(
                    new Executors.PlainExecutor(), () -> "shot.png");
            StepResult failed = failing.execute(new TestStep("bad", () -> false));
            assertEquals(List.of("shot.png"), failed.artifacts());
            assertEquals("assertion failed", failed.failureReason());

            // Chup screenshot LOI: van giu reason goc, chi ghi them ghi chu.
            var brokenCamera = new Executors.ScreenshotOnFailureExecutor(
                    new Executors.PlainExecutor(), () -> {
                        throw new IllegalStateException("no display");
                    });
            StepResult stillFailed = brokenCamera.execute(new TestStep("bad", () -> false));
            assertEquals("assertion failed", stillFailed.failureReason(), "loi goc KHONG duoc che mat");
            assertTrue(stillFailed.artifacts().getFirst().startsWith("screenshot-failed"));
        }

        @Test
        @DisplayName("builder lap chuoi doc xuoi: decorate(core).with(timing).with(retry)")
        void builderComposesInReadableOrder() {
            var executor = Executors.decorate(new Executors.PlainExecutor())
                    .with(Executors.TimingExecutor::new)
                    .with(inner -> new Executors.RetryingExecutor(inner, 3))
                    .build();

            StepResult result = executor.execute(flakyStep("login", 1));
            assertTrue(result.passed());
            assertEquals(2, result.attempts());
        }

        @Test
        @DisplayName("maxAttempts < 1 la loi cau hinh")
        void invalidRetryConfigRejected() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Executors.RetryingExecutor(new Executors.PlainExecutor(), 0));
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================================================================
    // STRATEGY + ADAPTER — engine selection
    // ==================================================================

    @Nested
    @DisplayName("Strategy + Adapter - chon engine theo nang luc, khong switch")
    class EngineStrategy {

        private static EngineSelection.EngineSelector selector() {
            return new EngineSelection.EngineSelector(List.of(
                    new EngineSelection.SeleniumEngine(),
                    new EngineSelection.PlaywrightEngine(),
                    new EngineSelection.AppiumEngine()));
        }

        @Test
        @DisplayName("web desktop: chon Playwright vi preference cao hon Selenium")
        void picksHighestPreferenceAmongCapableEngines() {
            var engine = selector().select(Platform.WEB_DESKTOP, Set.of()).orElseThrow();
            assertEquals("playwright", engine.name());
        }

        @Test
        @DisplayName("yeu cau CDP -> Selenium bi loai du no support web desktop")
        void capabilityFilteringExcludesEngines() {
            var selector = selector();

            assertEquals(
                    "playwright",
                    selector.select(Platform.WEB_DESKTOP, Set.of(Capability.CDP)).orElseThrow().name());

            // Selenium khong co CDP -> neu chi co Selenium thi khong chon duoc.
            var seleniumOnly =
                    new EngineSelection.EngineSelector(List.of(new EngineSelection.SeleniumEngine()));
            assertTrue(seleniumOnly.select(Platform.WEB_DESKTOP, Set.of(Capability.CDP)).isEmpty());
        }

        @Test
        @DisplayName("native mobile -> Appium; khong co engine nao thi Optional.empty (KHONG nem)")
        void unsupportedPlatformReturnsEmpty() {
            var selector = selector();
            assertEquals(
                    "appium", selector.select(Platform.NATIVE_ANDROID, Set.of()).orElseThrow().name());

            // Khong engine nao khai bao API -> empty. Caller phai xu ly, khong phai crash.
            assertTrue(selector.select(Platform.API, Set.of()).isEmpty());
        }

        @Test
        @DisplayName("them engine moi KHONG sua selector (Open/Closed)")
        void addingEngineRequiresNoChangeToSelector() {
            // Engine moi cho API — dinh nghia ngay tai cho, khong sua bat ky file nao.
            var apiEngine = new EngineSelection.AbstractEngineAdapter(
                    "rest-assured", Set.of(Platform.API), Set.of(), 90) {
                @Override
                public String run(String script) {
                    return "rest:" + script;
                }
            };

            var selector = new EngineSelection.EngineSelector(List.of(
                    new EngineSelection.SeleniumEngine(),
                    new EngineSelection.PlaywrightEngine(),
                    apiEngine));

            assertEquals("rest-assured", selector.select(Platform.API, Set.of()).orElseThrow().name());
            assertEquals("rest:GET /health", selector.select(Platform.API, Set.of()).orElseThrow().run("GET /health"));
        }

        @Test
        @DisplayName("allFor tra ve theo do uu tien giam dan - dung khi can fallback engine")
        void allForIsOrderedByPreference() {
            assertEquals(
                    List.of("playwright", "selenium"),
                    selector().allFor(Platform.WEB_DESKTOP).stream()
                            .map(EngineSelection.TestEngine::name)
                            .toList());
        }
    }

    // ==================================================================
    // OBSERVER
    // ==================================================================

    @Nested
    @DisplayName("Observer - event bus")
    class Observer {

        @Test
        @DisplayName("MOT listener nem KHONG lam chet run, va loi duoc GIU LAI de bao cao")
        void throwingListenerIsIsolatedButNotSwallowed() {
            var bus = new TestEventBus();
            var metrics = new TestEventBus.MetricsListener();

            bus.register(event -> {
                throw new IllegalStateException("customer webhook returned 500");
            });
            bus.register(metrics);

            // KHONG nem — day la diem quan trong nhat.
            bus.publish(new TestEvent.StepPassed("run-1", "login", Duration.ofMillis(100)));

            // Listener tot van chay binh thuong.
            assertEquals(1, metrics.passed());

            // Loi khong bi nuot: duoc thu lai kem TEN listener va event gay loi.
            assertEquals(1, bus.failures().size());
            assertTrue(bus.failures().getFirst().cause() instanceof IllegalStateException);
        }

        @Test
        @DisplayName("listener tu unregister minh trong luc dang duoc goi -> KHONG ConcurrentModification")
        void listenerCanUnregisterItselfDuringDispatch() {
            var bus = new TestEventBus();
            var callCount = new AtomicInteger();

            // One-shot listener: chay 1 lan roi tu thao.
            var oneShot = new TestEventBus.TestEventListener() {
                @Override
                public void onEvent(TestEvent event) {
                    callCount.incrementAndGet();
                    bus.unregister(this); // voi ArrayList thi day la ConcurrentModificationException
                }
            };
            bus.register(oneShot);

            bus.publish(new TestEvent.RunStarted("run-1", 10));
            bus.publish(new TestEvent.RunStarted("run-1", 10));

            assertEquals(1, callCount.get(), "chi duoc goi dung 1 lan");
            assertEquals(0, bus.listenerCount());
            assertTrue(bus.failures().isEmpty());
        }

        @Test
        @DisplayName("MetricsListener dung pattern matching exhaustive, phan biet flaky voi passed")
        void metricsSeparateFlakyFromPassed() {
            var bus = new TestEventBus();
            var metrics = new TestEventBus.MetricsListener();
            bus.register(metrics);

            bus.publish(new TestEvent.RunStarted("r", 4));
            bus.publish(new TestEvent.StepPassed("r", "a", Duration.ofMillis(100)));
            bus.publish(new TestEvent.StepPassed("r", "b", Duration.ofMillis(50)));
            bus.publish(new TestEvent.StepFlaky("r", "c", 3));
            bus.publish(new TestEvent.StepFailed("r", "d", "timeout"));
            bus.publish(new TestEvent.RunFinished("r", 2, 1, 1));

            assertEquals(2, metrics.passed());
            assertEquals(1, metrics.failed());
            assertEquals(1, metrics.flaky(), "flaky KHONG duoc tinh la passed");
            assertEquals(Duration.ofMillis(150), metrics.totalDuration());
        }

        @Test
        @DisplayName("BatchingWebhookListener: gom event thay vi goi 1 HTTP moi event")
        void batchingReducesDownstreamLoad() {
            var bus = new TestEventBus();
            var webhook = new TestEventBus.BatchingWebhookListener(3);
            bus.register(webhook);

            for (int i = 0; i < 7; i++) {
                bus.publish(new TestEvent.StepPassed("r", "step-" + i, Duration.ofMillis(1)));
            }

            // 7 event, batch 3 -> da flush 2 batch (3+3), con 1 pending.
            assertEquals(2, webhook.flushedBatches().size());
            assertEquals(1, webhook.pending());

            // RunFinished BUOC phai flush phan con lai, neu khong batch cuoi bi mat vinh vien.
            bus.publish(new TestEvent.RunFinished("r", 7, 0, 0));
            assertEquals(3, webhook.flushedBatches().size());
            assertEquals(0, webhook.pending(), "khong duoc de sot event trong buffer");
        }

        @Test
        @DisplayName("listener chi quan tam 1 loai event - instanceof pattern voi record deconstruction")
        void selectiveListener() {
            var bus = new TestEventBus();
            var alerts = new TestEventBus.FailureAlertListener();
            bus.register(alerts);

            bus.publish(new TestEvent.StepPassed("r", "a", Duration.ZERO));
            bus.publish(new TestEvent.StepFailed("r", "b", "element not found"));

            assertEquals(List.of("r/b: element not found"), alerts.alerts());
        }
    }

    // ==================================================================
    // TEMPLATE METHOD + BUILDER
    // ==================================================================

    @Nested
    @DisplayName("Template Method - teardown LUON chay")
    class Lifecycle {

        /** Test lifecycle ghi lai thu tu goi de assert duoc. */
        private static final class Recording extends TestLifecycle {
            private final List<String> calls = new ArrayList<>();
            private final RuntimeException setUpError;
            private final RuntimeException executeError;
            private final RuntimeException tearDownError;

            Recording(RuntimeException setUpError, RuntimeException executeError, RuntimeException tearDownError) {
                this.setUpError = setUpError;
                this.executeError = executeError;
                this.tearDownError = tearDownError;
            }

            @Override
            protected String testName() {
                return "recording";
            }

            @Override
            protected void setUp() {
                calls.add("setUp");
                if (setUpError != null) {
                    throw setUpError;
                }
            }

            @Override
            protected void execute() {
                calls.add("execute");
                if (executeError != null) {
                    throw executeError;
                }
            }

            @Override
            protected void tearDown() {
                calls.add("tearDown");
                if (tearDownError != null) {
                    throw tearDownError;
                }
            }
        }

        @Test
        @DisplayName("duong happy: setUp -> execute -> tearDown, passed")
        void happyPath() {
            var test = new Recording(null, null, null);
            var outcome = test.run();

            assertEquals(List.of("setUp", "execute", "tearDown"), test.calls);
            assertTrue(outcome.passed());
            assertTrue(outcome.teardownRan());
        }

        @Test
        @DisplayName("execute NEM -> tearDown VAN chay (chong ro ri browser/container)")
        void teardownRunsWhenExecuteThrows() {
            var test = new Recording(null, new IllegalStateException("assertion blew up"), null);
            var outcome = test.run();

            assertEquals(List.of("setUp", "execute", "tearDown"), test.calls);
            assertFalse(outcome.passed());
            assertTrue(outcome.teardownRan(), "day la bug so 1 ma Template Method sinh ra de chan");
            assertTrue(outcome.errors().getFirst().contains("assertion blew up"));
        }

        @Test
        @DisplayName("setUp NEM -> KHONG chay execute, nhung tearDown VAN chay")
        void setUpFailureSkipsExecuteButStillTearsDown() {
            var test = new Recording(new IllegalStateException("browser won't start"), null, null);
            var outcome = test.run();

            // Khong co "execute": chay than test tren trang thai nua voi chi sinh loi thu hai
            // che mat loi thuc su.
            assertEquals(List.of("setUp", "tearDown"), test.calls);
            assertFalse(outcome.passed());
            assertTrue(outcome.teardownRan());
        }

        @Test
        @DisplayName("tearDown NEM -> KHONG che mat loi goc cua execute")
        void teardownFailureDoesNotMaskTheOriginalError() {
            var test = new Recording(
                    null, new IllegalStateException("real bug"), new IllegalStateException("cleanup failed"));
            var outcome = test.run();

            assertFalse(outcome.passed());
            // CA HAI loi deu duoc giu. Neu chi `throw` trong finally thi "real bug" bien mat
            // va ban se debug sai cho hang gio — bay "exception masking".
            assertEquals(2, outcome.errors().size());
            assertTrue(outcome.errors().stream().anyMatch(e -> e.contains("real bug")));
            assertTrue(outcome.errors().stream().anyMatch(e -> e.contains("cleanup failed")));
        }

        @Test
        @DisplayName("tearDown loi nhung test pass -> van tinh la PASS (teardown khong quyet dinh ket qua)")
        void teardownFailureDoesNotFailAPassingTest() {
            var test = new Recording(null, null, new IllegalStateException("cleanup failed"));
            var outcome = test.run();

            // Quyet dinh thiet ke co the tranh luan: teardown loi la van de HA TANG, khong phai
            // that bai cua san pham duoc test. Bao fail se gay bao dong gia.
            // Diem quan trong la loi VAN duoc ghi lai chu khong bi nuot.
            assertTrue(outcome.passed());
            assertEquals(1, outcome.errors().size());
        }
    }

    @Nested
    @DisplayName("Builder - validate mot lan, bao ca cum loi")
    class BuilderPattern {

        @Test
        @DisplayName("default hop ly, build thanh cong, ket qua immutable")
        void defaultsAreUsable() {
            var testCase = TestCase.builder("login test").tag("owner", "duc").build();

            assertEquals("default", testCase.suite());
            assertEquals(TestCase.Platform.WEB, testCase.platform());
            assertEquals(Duration.ofSeconds(30), testCase.timeout());
            assertEquals(0, testCase.maxRetries());
            assertThrows(UnsupportedOperationException.class, () -> testCase.tags().put("x", "y"));
        }

        @Test
        @DisplayName("bao CA CUM loi mot lan, khong bat nguoi dung sua 5 vong")
        void allProblemsReportedAtOnce() {
            var ex = assertThrows(
                    IllegalStateException.class,
                    () -> TestCase.builder("  ").timeout(Duration.ZERO).maxRetries(-1).build());

            String message = ex.getMessage();
            assertTrue(message.contains("name must not be blank"), message);
            assertTrue(message.contains("timeout must be positive"), message);
            assertTrue(message.contains("maxRetries must be >= 0"), message);
        }

        @Test
        @DisplayName("rang buoc LIEN-TRUONG chi kiem tra duoc trong build(), khong trong setter")
        void crossFieldConstraint() {
            // Rieng le thi ca hai deu hop le: timeout 400s ok, retries 2 ok.
            // Ket hop lai thi vo ly (400s x 3 attempt = 20 phut cho 1 test).
            var ex = assertThrows(
                    IllegalStateException.class,
                    () -> TestCase.builder("slow").timeout(Duration.ofSeconds(400)).maxRetries(2).build());
            assertTrue(ex.getMessage().contains("exceed the run budget"), ex.getMessage());

            // Bo retries thi hop le.
            assertEquals(
                    Duration.ofSeconds(400),
                    TestCase.builder("slow").timeout(Duration.ofSeconds(400)).build().timeout());
        }

        @Test
        @DisplayName("rang buoc theo platform: API test khong duoc co tag browser")
        void platformSpecificConstraint() {
            var ex = assertThrows(
                    IllegalStateException.class,
                    () -> TestCase.builder("api")
                            .platform(TestCase.Platform.API)
                            .tag("browser", "chrome")
                            .build());
            assertTrue(ex.getMessage().contains("must not declare a 'browser' tag"), ex.getMessage());
        }
    }
}
