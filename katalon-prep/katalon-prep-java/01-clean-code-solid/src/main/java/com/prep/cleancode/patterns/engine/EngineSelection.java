package com.prep.cleancode.patterns.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ============================================================================
 * STRATEGY + ADAPTER — chon engine chay test theo NANG LUC, khong theo switch.
 * ============================================================================
 *
 * <p>Checklist goc cua ban ghi: <i>"Strategy (vi du: thay doi linh hoat giua cac bo chay test
 * Selenium, Appium, Playwright)"</i>. Dung — va day la ban implement dung cach.
 *
 * <p><b>Cach SAI ma hau het nguoi ta lam</b> (va la cach {@code before/ServiceHealthAggregatorBefore}
 * dang lam):
 * <pre>{@code
 * switch (platform) {
 *     case WEB    -> seleniumEngine.run(step);
 *     case MOBILE -> appiumEngine.run(step);
 *     ...
 * }
 * }</pre>
 * Them engine moi = sua switch. Vi pham Open/Closed.
 *
 * <p><b>Cach dung:</b> engine tu khai bao no lam duoc gi ({@code supportedPlatforms}), selector chi
 * viec loc + sap xep theo do uu tien. Them engine moi = them mot class, khong sua gi.
 */
public final class EngineSelection {

    private EngineSelection() {}

    public enum Platform {
        WEB_DESKTOP,
        WEB_MOBILE,
        NATIVE_ANDROID,
        NATIVE_IOS,
        API
    }

    public enum Capability {
        /** Chan duoc network request (intercept/mock). */
        NETWORK_INTERCEPTION,
        /** Truy cap Chrome DevTools Protocol — can cho instrumentation. */
        CDP,
        /** Chay duoc khong can hien thi. */
        HEADLESS,
        /** Ghi lai video/trace de debug. */
        TRACING,
        /** Tu doi (auto-wait) truoc khi tuong tac. */
        AUTO_WAIT
    }

    /**
     * Strategy interface. Ba method dau la <b>metadata de chon</b>, method cuoi moi la hanh dong.
     *
     * <p>Tach metadata ra khoi hanh dong la meo lam cho Strategy tu dang ky duoc — giong
     * {@code HealthCheck.service()} o module truoc.
     */
    public interface TestEngine {

        String name();

        Set<Platform> supportedPlatforms();

        Set<Capability> capabilities();

        /**
         * Do uu tien khi nhieu engine cung lam duoc. Cao hon = duoc chon truoc.
         * Vi du: Playwright > Selenium cho web vi nhanh hon va co auto-wait.
         */
        int preference();

        String run(String script);
    }

    /**
     * ADAPTER — boc mot thu vien ben ngoai (API cua no khong theo y ta) vao interface cua ta.
     *
     * <p>Vi sao can Adapter o day: Selenium, Appium, Playwright co API hoan toan khac nhau
     * ({@code findElement} vs {@code locator()} vs {@code $()}). Neu code test goi truc tiep API cua
     * tung thu vien, doi engine = viet lai toan bo test. Adapter khoanh su khac biet do vao mot cho.
     *
     * <p>Day cung la lop bao ve khi thu vien ben ngoai co breaking change: chi mot file phai sua.
     */
    public abstract static class AbstractEngineAdapter implements TestEngine {
        private final String name;
        private final Set<Platform> platforms;
        private final Set<Capability> capabilities;
        private final int preference;

        protected AbstractEngineAdapter(
                String name, Set<Platform> platforms, Set<Capability> capabilities, int preference) {
            this.name = name;
            this.platforms = Set.copyOf(platforms);
            this.capabilities = Set.copyOf(capabilities);
            this.preference = preference;
        }

        @Override
        public final String name() {
            return name;
        }

        @Override
        public final Set<Platform> supportedPlatforms() {
            return platforms;
        }

        @Override
        public final Set<Capability> capabilities() {
            return capabilities;
        }

        @Override
        public final int preference() {
            return preference;
        }
    }

    /** Playwright: manh nhat cho web, co CDP + auto-wait + tracing. */
    public static final class PlaywrightEngine extends AbstractEngineAdapter {
        public PlaywrightEngine() {
            super(
                    "playwright",
                    EnumSet.of(Platform.WEB_DESKTOP, Platform.WEB_MOBILE),
                    EnumSet.of(
                            Capability.NETWORK_INTERCEPTION,
                            Capability.CDP,
                            Capability.HEADLESS,
                            Capability.TRACING,
                            Capability.AUTO_WAIT),
                    100);
        }

        @Override
        public String run(String script) {
            return "playwright:" + script;
        }
    }

    /** Selenium: pho bien nhat, nhung khong co auto-wait va khong CDP day du. */
    public static final class SeleniumEngine extends AbstractEngineAdapter {
        public SeleniumEngine() {
            super(
                    "selenium",
                    EnumSet.of(Platform.WEB_DESKTOP),
                    EnumSet.of(Capability.HEADLESS),
                    50);
        }

        @Override
        public String run(String script) {
            return "selenium:" + script;
        }
    }

    /** Appium: chi native mobile. */
    public static final class AppiumEngine extends AbstractEngineAdapter {
        public AppiumEngine() {
            super(
                    "appium",
                    EnumSet.of(Platform.NATIVE_ANDROID, Platform.NATIVE_IOS),
                    EnumSet.of(Capability.TRACING),
                    70);
        }

        @Override
        public String run(String script) {
            return "appium:" + script;
        }
    }

    /**
     * Selector: loc theo platform + capability bat buoc, sap xep theo preference.
     *
     * <p>KHONG co switch. Them engine = them phan tu vao list truyen vao constructor
     * (trong Spring: {@code List<TestEngine>} tu duoc inject).
     */
    public static final class EngineSelector {

        private final List<TestEngine> engines;

        public EngineSelector(List<TestEngine> engines) {
            if (engines == null || engines.isEmpty()) {
                throw new IllegalArgumentException("at least one engine must be registered");
            }
            var sorted = new ArrayList<>(engines);
            // Sap xep 1 lan luc khoi tao, khong sap xep lai moi request.
            sorted.sort(Comparator.comparingInt(TestEngine::preference).reversed());
            this.engines = List.copyOf(sorted);
        }

        /**
         * Engine tot nhat chay duoc {@code platform} va co DU cac capability yeu cau.
         *
         * <p>Tra {@code Optional} chu khong nem: "khong co engine phu hop" la thong tin hop le ma
         * caller phai xu ly (bao cho user biet ho can cai gi), khong phai loi lap trinh.
         */
        public Optional<TestEngine> select(Platform platform, Set<Capability> required) {
            Set<Capability> needed = required == null ? Set.of() : required;
            return engines.stream()
                    .filter(engine -> engine.supportedPlatforms().contains(platform))
                    .filter(engine -> engine.capabilities().containsAll(needed))
                    .findFirst(); // da sort theo preference nen first = tot nhat
        }

        /** Tat ca engine chay duoc platform nay, tot nhat truoc — dung khi can fallback. */
        public List<TestEngine> allFor(Platform platform) {
            return engines.stream()
                    .filter(engine -> engine.supportedPlatforms().contains(platform))
                    .toList();
        }

        public List<String> registeredNames() {
            return engines.stream().map(TestEngine::name).toList();
        }
    }
}
