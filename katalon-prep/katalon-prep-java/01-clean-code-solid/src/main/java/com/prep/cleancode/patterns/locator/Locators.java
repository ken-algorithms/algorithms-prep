package com.prep.cleancode.patterns.locator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * CHAIN OF RESPONSIBILITY — self-healing locator.
 * ============================================================================
 *
 * <p>Day la bai toan DAC TRUNG NHAT cua Katalon, va la thu JD goi la
 * <i>"web instrumentation and browser automation"</i>. Neu chi chuan bi mot pattern de ke trong
 * phong van, chuan bi cai nay.
 *
 * <p><b>Bai toan:</b> test tim element bang CSS selector {@code #submit-btn}. Dev doi UI, id bien
 * mat. Test fail — nhung khong phai vi san pham loi, ma vi locator gion. Do la nguyen nhan so 1
 * cua test automation bi bo hoang o moi cong ty.
 *
 * <p><b>Giai phap:</b> khong dung MOT locator, dung mot CHUOI locator xep theo do ben. Thu lan
 * luot tu ben nhat den mong manh nhat, dung lai o cai dau tien tim thay. Neu phai dung cai thu 2
 * tro di, ghi nhan lai la da "chua" (healed) kem confidence de nguoi ta biet ma cap nhat test.
 *
 * <p><b>Vi sao Chain of Responsibility, khong phai if-else?</b> Vi chuoi phai <b>cau hinh duoc</b>
 * (moi tenant co convention khac nhau), <b>mo rong duoc</b> (them chien luoc moi khong sua code cu),
 * va phai <b>bao cao duoc</b> mat xich nao da xu ly. if-else khong cho ba thu do.
 */
public final class Locators {

    private Locators() {}

    /**
     * Sealed hierarchy cac cach dinh vi element, xep theo DO BEN giam dan.
     *
     * <p>{@code stability()} la tri thuc nghiep vu that: {@code data-testid} do QA/dev chu dong dat
     * nen gan nhu khong bao gio doi; ARIA role gan voi y nghia nen kha ben; text nhin thay duoc thi
     * doi khi i18n; CSS class doi moi lan refactor style; XPath theo vi tri thi vo ngay khi them mot
     * the div.
     */
    public sealed interface Locator {

        /** 100 = ben nhat. Dung de sap xep chuoi va tinh confidence. */
        int stability();

        String describe();

        record TestId(String value) implements Locator {
            @Override
            public int stability() {
                return 100;
            }

            @Override
            public String describe() {
                return "[data-testid='%s']".formatted(value);
            }
        }

        record Role(String role, String accessibleName) implements Locator {
            @Override
            public int stability() {
                return 80;
            }

            @Override
            public String describe() {
                return "role=%s[name='%s']".formatted(role, accessibleName);
            }
        }

        record Text(String exactText) implements Locator {
            @Override
            public int stability() {
                return 60;
            }

            @Override
            public String describe() {
                return "text='%s'".formatted(exactText);
            }
        }

        record Css(String selector) implements Locator {
            @Override
            public int stability() {
                return 40;
            }

            @Override
            public String describe() {
                return "css=%s".formatted(selector);
            }
        }

        /** XPath theo vi tri tuyet doi — mong manh nhat, chi dung khi het cach. */
        record PositionalXPath(String xpath) implements Locator {
            @Override
            public int stability() {
                return 10;
            }

            @Override
            public String describe() {
                return "xpath=%s".formatted(xpath);
            }
        }
    }

    /** Element tim duoc. Trong thuc te la handle tro vao DOM node. */
    public record Element(String tag, String innerText) {}

    /**
     * Trang web toi gian de test duoc khong can browser.
     *
     * <p>Day la DIP: {@code SelfHealingLocator} chi phu thuoc interface nay, nen test chay bang fake.
     * Ban that se co implementation goi Playwright / CDP.
     */
    public interface Page {
        /** Tra ve element neu locator match, rong neu khong. KHONG duoc nem. */
        Optional<Element> query(Locator locator);
    }

    /**
     * Ket qua phan giai — ghi lai DU thong tin de van hanh, khong chi tra ve element.
     *
     * @param element element tim duoc
     * @param used locator da dung thanh cong
     * @param attemptIndex vi tri trong chuoi (0 = locator uu tien nhat)
     * @param healed true neu phai dung locator du phong (attemptIndex > 0)
     * @param confidence 0..100 — do tin cay cua ket qua, giam dan theo do sau trong chuoi
     * @param triedAndFailed cac locator da thu ma khong thay — du lieu de de xuat cap nhat test
     */
    public record Resolution(
            Element element,
            Locator used,
            int attemptIndex,
            boolean healed,
            int confidence,
            List<Locator> triedAndFailed) {

        public Resolution {
            triedAndFailed = List.copyOf(triedAndFailed);
        }

        /**
         * Nguong de tu dong dung ket qua. Duoi nguong thi van tra ve, nhung phai cho nguoi xac nhan.
         *
         * <p>Day la diem PHAN BIET LEAD: khong bao gio auto-update test suite cua khach bang mot
         * locator ma he thong chi doan duoc voi confidence thap. Sai mot lan la mat niem tin vinh vien.
         */
        public boolean trustworthyEnoughToAutoUpdate() {
            return confidence >= 70;
        }
    }

    /** Khong tim thay bang bat ky locator nao trong chuoi. */
    public static final class ElementNotFoundException extends RuntimeException {
        private final transient List<Locator> tried;

        ElementNotFoundException(String message, List<Locator> tried) {
            super(message);
            this.tried = List.copyOf(tried);
        }

        public List<Locator> tried() {
            return tried;
        }
    }

    /**
     * CHUOI trach nhiem. Immutable, thread-safe, cau hinh duoc.
     *
     * <p>Chu y thiet ke: class nay KHONG biet Playwright, KHONG biet CDP, KHONG biet HTTP. No chi
     * biet {@link Page}. Nho vay toan bo logic self-healing — phan kho nhat va dang test nhat —
     * duoc test bang plain JUnit, khong can browser. Do la Dependency Inversion tra cong.
     */
    public static final class SelfHealingLocator {

        private final List<Locator> chain;
        private final HealingTelemetry telemetry;

        /**
         * @param chain danh sach locator, se duoc TU DONG sap xep theo stability giam dan
         * @param telemetry noi ghi nhan viec "da chua" — de dashboard biet test nao dang muc
         */
        public SelfHealingLocator(List<Locator> chain, HealingTelemetry telemetry) {
            if (chain == null || chain.isEmpty()) {
                throw new IllegalArgumentException("locator chain must not be empty");
            }
            // Sap xep de nguoi viet test khong phai nho thu tu — he thong tu biet cai nao ben hon.
            // Dung List.copyOf sau khi sort de bat bien.
            var sorted = new ArrayList<>(chain);
            sorted.sort((a, b) -> Integer.compare(b.stability(), a.stability()));
            this.chain = List.copyOf(sorted);
            this.telemetry = telemetry == null ? HealingTelemetry.noop() : telemetry;
        }

        public Resolution resolve(Page page) {
            var failed = new ArrayList<Locator>();

            for (int i = 0; i < chain.size(); i++) {
                Locator locator = chain.get(i);
                Optional<Element> found = safeQuery(page, locator);

                if (found.isPresent()) {
                    boolean healed = i > 0;
                    int confidence = confidenceFor(locator, i);
                    if (healed) {
                        // Ghi nhan NGAY — day la du lieu de de xuat "test cua ban nen doi sang X".
                        telemetry.recordHealed(chain.getFirst(), locator, confidence);
                    }
                    return new Resolution(found.get(), locator, i, healed, confidence, failed);
                }
                failed.add(locator);
            }

            telemetry.recordUnresolved(chain);
            throw new ElementNotFoundException(
                    "none of %d locators matched: %s"
                            .formatted(chain.size(), chain.stream().map(Locator::describe).toList()),
                    failed);
        }

        /**
         * Implementation cua Page co the nem (bug, hoac browser mat ket noi). Mot locator loi KHONG
         * duoc lam chet ca chuoi — coi nhu khong tim thay va di tiep.
         *
         * <p>Cung mot bai hoc voi {@code HealthAggregator.guarded()}: cuong che hop dong o bien.
         */
        private Optional<Element> safeQuery(Page page, Locator locator) {
            try {
                Optional<Element> result = page.query(locator);
                return result == null ? Optional.empty() : result;
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }

        /**
         * Confidence = do ben cua locator da dung, tru hinh phat theo do sau trong chuoi.
         *
         * <p>Vi sao tru theo do sau: phai xuong sau nghia la cac locator uu tien da vo, tuc la DOM
         * doi nhieu hon du kien — ket qua dang tin it hon du locator hien tai co ve on.
         */
        private static int confidenceFor(Locator locator, int attemptIndex) {
            int penalty = attemptIndex * 15;
            return Math.max(0, Math.min(100, locator.stability() - penalty));
        }

        public List<Locator> chain() {
            return chain;
        }
    }

    /** Cho phep cam metrics/log vao ma khong lam SelfHealingLocator phu thuoc vao ha tang. */
    public interface HealingTelemetry {
        void recordHealed(Locator preferred, Locator actuallyUsed, int confidence);

        void recordUnresolved(List<Locator> chain);

        static HealingTelemetry noop() {
            return new HealingTelemetry() {
                @Override
                public void recordHealed(Locator preferred, Locator actuallyUsed, int confidence) {
                    // khong lam gi
                }

                @Override
                public void recordUnresolved(List<Locator> chain) {
                    // khong lam gi
                }
            };
        }
    }

    /** Telemetry dung trong test va trong bao cao — dem so lan chua theo tung locator goc. */
    public static final class InMemoryTelemetry implements HealingTelemetry {
        private final Map<String, Integer> healCounts = new LinkedHashMap<>();
        private int unresolvedCount;

        @Override
        public void recordHealed(Locator preferred, Locator actuallyUsed, int confidence) {
            healCounts.merge(preferred.describe() + " -> " + actuallyUsed.describe(), 1, Integer::sum);
        }

        @Override
        public void recordUnresolved(List<Locator> chain) {
            unresolvedCount++;
        }

        public Map<String, Integer> healCounts() {
            return Map.copyOf(healCounts);
        }

        public int unresolvedCount() {
            return unresolvedCount;
        }
    }
}
