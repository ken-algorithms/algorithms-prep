package com.prep.perf.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * P15 - N+1 query (va ho hang cua no: goi HTTP trong vong lap).
 *
 * <pre>
 * XAU   List&lt;Order&gt; orders = orderRepo.findRecent(100);       // 1 query
 *       orders.forEach(o -&gt; o.getItems().size());               // LAZY: +1 query MOI order
 * SUA   @EntityGraph / JOIN FETCH, hoac 1 query IN (:ids) roi gom nhom trong Java  // 2 query
 * </pre>
 *
 * FakeDb dem so round trip va ngu {@code rttMs} moi query - chinh la chi phi mang + parse + plan ma
 * Postgres that phai tra cho moi query, ke ca query tra 1 dong. Con so query la DEM (chinh xac);
 * thoi gian la mo phong RTT (0.5-1 ms la binh thuong trong cung AZ).
 */
public final class D15NPlusOne {

    private D15NPlusOne() {}

    public record Order(long id, long customerId) {}

    public record Item(long orderId, String sku, long amountMinor) {}

    public record OrderView(long orderId, int itemCount, long totalMinor) {}

    public static final class FakeDb {
        private final long rttMs;
        private final AtomicInteger queries = new AtomicInteger();
        private final Map<Long, List<Item>> itemsByOrder = new HashMap<>();
        private final List<Order> orders = new ArrayList<>();

        public FakeDb(int orderCount, int itemsPerOrder, long rttMs) {
            this.rttMs = rttMs;
            for (long o = 1; o <= orderCount; o++) {
                orders.add(new Order(o, 1000 + o % 37));
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < itemsPerOrder; i++) {
                    items.add(new Item(o, "SKU-" + (o * 7 + i) % 500, 10_000L * (i + 1)));
                }
                itemsByOrder.put(o, items);
            }
        }

        private void roundTrip() {
            queries.incrementAndGet();
            Lat.sleepMs(rttMs);
        }

        public List<Order> findRecentOrders(int limit) {
            roundTrip();
            return orders.subList(0, Math.min(limit, orders.size()));
        }

        public List<Item> findItemsByOrderId(long orderId) {
            roundTrip();
            return itemsByOrder.getOrDefault(orderId, List.of());
        }

        /** SELECT ... WHERE order_id IN (:ids) - 1 round trip cho ca lo. */
        public List<Item> findItemsByOrderIds(List<Long> ids) {
            roundTrip();
            return ids.stream().flatMap(id -> itemsByOrder.getOrDefault(id, List.of()).stream()).toList();
        }

        public int queries() {
            return queries.get();
        }
    }

    /** XAU: 1 + N query. */
    public static List<OrderView> loadBad(FakeDb db, int limit) {
        List<OrderView> out = new ArrayList<>();
        for (Order o : db.findRecentOrders(limit)) {
            List<Item> items = db.findItemsByOrderId(o.id());
            out.add(new OrderView(o.id(), items.size(), items.stream().mapToLong(Item::amountMinor).sum()));
        }
        return out;
    }

    /** SUA: 2 query, gom nhom trong bo nho. */
    public static List<OrderView> loadGood(FakeDb db, int limit) {
        List<Order> orders = db.findRecentOrders(limit);
        Map<Long, List<Item>> byOrder = db.findItemsByOrderIds(orders.stream().map(Order::id).toList())
                .stream().collect(Collectors.groupingBy(Item::orderId));
        List<OrderView> out = new ArrayList<>();
        for (Order o : orders) {
            List<Item> items = byOrder.getOrDefault(o.id(), List.of());
            out.add(new OrderView(o.id(), items.size(), items.stream().mapToLong(Item::amountMinor).sum()));
        }
        return out;
    }

    public static void main(String[] args) {
        int limit = 100;
        long rtt = 1;
        System.out.printf("trang 'don hang gan day': %d order, RTT moi query %d ms%n", limit, rtt);
        var bad = new FakeDb(500, 3, rtt);
        long t0 = System.nanoTime();
        var r1 = loadBad(bad, limit);
        long badMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        var good = new FakeDb(500, 3, rtt);
        t0 = System.nanoTime();
        var r2 = loadGood(good, limit);
        long goodMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        System.out.printf("N+1     : queries=%d  time=%dms%n", bad.queries(), badMs);
        System.out.printf("batched : queries=%d  time=%dms%n", good.queries(), goodMs);
        System.out.printf("same result: %s%n", r1.equals(r2));
        System.out.printf("o 200 req/s: N+1 = %,d query/s vao DB, batched = %,d query/s%n",
                bad.queries() * 200, good.queries() * 200);
    }
}
