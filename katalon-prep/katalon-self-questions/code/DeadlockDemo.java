import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;

public class DeadlockDemo {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) throws Exception {
        // Latch makes the deadlock DETERMINISTIC: both threads must hold their
        // first lock before either is allowed to reach for the second one.
        CountDownLatch bothHoldFirst = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {                 // 1st: A
                sync(bothHoldFirst);
                synchronized (LOCK_B) {             // 2nd: B  -- never reached
                    System.out.println("t1 finished");
                }
            }
        }, "worker-1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {                 // 1st: B   <-- REVERSED ORDER
                sync(bothHoldFirst);
                synchronized (LOCK_A) {             // 2nd: A  -- never reached
                    System.out.println("t2 finished");
                }
            }
        }, "worker-2");

        t1.setDaemon(true); t2.setDaemon(true);     // daemon so the JVM can still exit
        t1.start(); t2.start();

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = null;
        for (int i = 0; i < 30 && ids == null; i++) {
            Thread.sleep(100);
            ids = bean.findDeadlockedThreads();     // the JVM can DETECT it, but never FIX it
        }

        if (ids == null) {
            System.out.println("NO deadlock detected");
        } else {
            System.out.println("DEADLOCK detected, threads = " + ids.length);
            for (ThreadInfo info : bean.getThreadInfo(ids)) {
                System.out.printf("  %s  blocked on <%s>  held by %s%n",
                        info.getThreadName(), info.getLockName(), info.getLockOwnerName());
            }
        }
        System.out.println("main exits; the two workers are stuck forever");
    }

    private static void sync(CountDownLatch latch) {
        try { latch.countDown(); latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
