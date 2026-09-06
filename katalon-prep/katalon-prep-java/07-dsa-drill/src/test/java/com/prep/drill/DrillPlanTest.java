package com.prep.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test cho ban than lich luyen — luon xanh, dung de in ke hoach ra man hinh. */
class DrillPlanTest {

    @Test
    @DisplayName("in lich 5 tuan + format bat buoc")
    void printPlan() {
        var plan = Drill.fiveWeekPlan();
        assertEquals(5, plan.size());

        System.out.println("=".repeat(78));
        System.out.println("LICH DRILL 5 TUAN — TAT COPILOT, BAM GIO, NOI TO");
        System.out.println("=".repeat(78));
        for (int week = 0; week < plan.size(); week++) {
            int minutes = plan.get(week).stream().mapToInt(Drill.Problem::minutes).sum();
            System.out.printf("%nTUAN %d  (%d phut)%n", week + 1, minutes);
            for (var problem : plan.get(week)) {
                System.out.printf(
                        "  #%-4d %-46s %2d'  [%s]%n      -> %s%n",
                        problem.leetcodeId(), problem.title(), problem.minutes(),
                        problem.pattern(), problem.katalonRelevance());
            }
        }
        System.out.println("\n" + "-".repeat(78));
        System.out.println("FORMAT BAT BUOC cho MOI bai:");
        Drill.requiredFormat().forEach(step -> System.out.println("  " + step));
        System.out.println("-".repeat(78));

        int total = Drill.onDomainSubset().stream().mapToInt(Drill.Problem::minutes).sum();
        System.out.printf("Tong: %d bai, %d phut (~%.1f gio)%n",
                Drill.onDomainSubset().size(), total, total / 60.0);
    }

    @Test
    @DisplayName("subset on-domain: 20 bai, khong trung, moi bai co lien he Katalon")
    void subsetIsWellFormed() {
        var problems = Drill.onDomainSubset();
        assertEquals(20, problems.size());
        assertEquals(
                20,
                problems.stream().map(Drill.Problem::leetcodeId).distinct().count(),
                "khong duoc trung bai");
        assertTrue(
                problems.stream().allMatch(p -> !p.katalonRelevance().isBlank()),
                "moi bai phai noi duoc lien he voi domain Katalon - do la thu giup nho lau");
        assertTrue(
                problems.stream().allMatch(p -> p.minutes() >= 10 && p.minutes() <= 35),
                "thoi luong phai thuc te");
    }

    @Test
    @DisplayName("5 tuan phu het 20 bai, khong sot bai nao")
    void planCoversEveryProblem() {
        var flattened = Drill.fiveWeekPlan().stream().flatMap(java.util.List::stream).toList();
        assertEquals(Drill.onDomainSubset(), flattened);
    }
}
