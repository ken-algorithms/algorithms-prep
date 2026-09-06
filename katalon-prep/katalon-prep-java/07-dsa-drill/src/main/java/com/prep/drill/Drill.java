package com.prep.drill;

import java.util.List;

/**
 * ============================================================================
 * DRILL HARNESS — luyen 38 bai bang Java, BAM GIO, TAT COPILOT.
 * ============================================================================
 *
 * <p>Module nay KHONG chua loi giai. Loi giai da co day du o
 * {@code ../leetcode-38-bai-phong-van-vietnam.md} (ca Python va Java 21).
 *
 * <p>Muc dich khac han: tao mot cho de ban <b>viet lai tu dau, khong nhin, khong AI</b>, va co test
 * chay ngay de biet dung/sai. Day chinh la cach bit rui ro so 1 cua viec quen dung AI gen code:
 * <i>vong live coding khong co Copilot</i>.
 *
 * <p><b>Cach dung:</b>
 * <ol>
 *   <li>Tat Copilot/Cursor (Cmd+Shift+P -> "Disable Copilot completions").
 *   <li>Mo {@code DrillWorkspace.java}, chon mot bai, bam gio 25 phut.
 *   <li>Viet loi giai. NOI TO trong luc viet - do la ky nang duoc cham diem, khong phai code.
 *   <li>Chay {@code mvn -pl 07-dsa-drill test} de kiem tra.
 *   <li>Chi khi xong (hoac het 25 phut) moi mo file .md de doi chieu.
 * </ol>
 */
public final class Drill {

    private Drill() {}

    /** Mot bai trong lich luyen, kem lien he ve domain Katalon de nho lau hon. */
    public record Problem(
            int leetcodeId, String title, String pattern, String katalonRelevance, int minutes) {}

    /**
     * SUBSET ON-DOMAIN — 20/38 bai, chon theo do lien quan toi Katalon.
     *
     * <p>Khong luyen ca 38. Vong coding cua Katalon khong phai LeetCode-hard (xem tai lieu chien
     * luoc, muc 1.4): 3-4 vong, co take-home, noi dung la case study + discussion. DSA Medium chi
     * la dieu kien DE KHONG BI LOAI. Dung grind them 200 bai — dua thoi gian sang take-home.
     */
    public static List<Problem> onDomainSubset() {
        return List.of(
                // --- Tree / Graph: DOM tree, test suite tree, dependency giua test ---
                new Problem(102, "Binary Tree Level Order Traversal", "BFS",
                        "duyet DOM tree theo tang; render cay test suite", 20),
                new Problem(105, "Construct Binary Tree from Preorder and Inorder", "HashMap + Recursion",
                        "dung lai cay DOM tu snapshot", 25),
                new Problem(236, "Lowest Common Ancestor", "Tree DFS",
                        "tim container chung gan nhat cua 2 element - dung cho self-healing locator", 20),
                new Problem(98, "Validate Binary Search Tree", "DFS + range",
                        "kiem tra bat bien tren cay; bay so sanh voi cha truc tiep", 20),
                new Problem(297, "Serialize and Deserialize Binary Tree", "DFS preorder",
                        "luu/khoi phuc DOM snapshot", 25),
                new Problem(207, "Course Schedule", "Topological Sort",
                        "THU TU PHU THUOC GIUA TEST - bai on-domain nhat cua nhom nay", 25),

                // --- Hash / Prefix sum: dedupe event, gom session ---
                new Problem(1, "Two Sum", "HashMap", "khoi dong; phai giai duoi 10 phut", 10),
                new Problem(560, "Subarray Sum Equals K", "Prefix Sum + HashMap",
                        "dem cua so co tong bang k trong chuoi event", 25),
                new Problem(523, "Continuous Subarray Sum", "Prefix Sum % k",
                        "phat hien chu ky trong log", 25),
                new Problem(49, "Group Anagrams", "HashMap",
                        "gom test theo chu ky chuan hoa (normalised signature)", 15),
                new Problem(128, "Longest Consecutive Sequence", "HashSet",
                        "tim chuoi hanh dong lien tiep trong journey", 20),

                // --- Sliding window: phan tich log theo cua so thoi gian ---
                new Problem(3, "Longest Substring Without Repeating Characters", "Sliding Window",
                        "cua so khong lap trong chuoi su kien", 20),
                new Problem(76, "Minimum Window Substring", "Sliding Window",
                        "cua so nho nhat chua du tap hanh dong - HARD, de cuoi", 35),

                // --- Heap: scheduling ---
                new Problem(253, "Meeting Rooms II", "Heap / Sorting",
                        "XEP TEST SONG SONG VAO EXECUTOR POOL - noi duoc lien he nay la diem cong lon", 25),
                new Problem(215, "Kth Largest Element", "Heap",
                        "top-k test cham nhat", 15),
                new Problem(347, "Top K Frequent Elements", "HashMap + Heap",
                        "top-k loi hay gap nhat trong bao cao", 20),

                // --- Design: hieu ban chat cau truc du lieu ---
                new Problem(146, "LRU Cache", "HashMap + Doubly Linked List",
                        "cache DOM snapshot / ket qua locator", 30),
                new Problem(706, "Design HashMap", "Separate chaining",
                        "hieu ben trong HashMap - hay bi hoi khi noi ve hash", 25),

                // --- Two pointers / binary search ---
                new Problem(19, "Remove Nth Node From End of List", "Two Pointers",
                        "thao tac linked list tai cho", 15),
                new Problem(33, "Search in Rotated Sorted Array", "Binary Search",
                        "binary search co bien the - de sai off-by-one khi hoi hop", 20));
    }

    /** Lich 5 tuan, moi tuan 4 bai. Tong ~7 gio, rai deu thay vi don mot dot. */
    public static List<List<Problem>> fiveWeekPlan() {
        List<Problem> all = onDomainSubset();
        return List.of(
                all.subList(0, 4),
                all.subList(4, 8),
                all.subList(8, 12),
                all.subList(12, 16),
                all.subList(16, 20));
    }

    /**
     * FORMAT BAT BUOC cho moi bai — vi day moi la thu duoc cham diem, khong phai dap an.
     *
     * <p>Interviewer o vong coding danh gia QUA TRINH nhieu hon KET QUA. Mot ung vien clarify tot,
     * neu duoc naive roi toi uu, va noi to suot qua trinh — se qua vong ke ca khi khong kip xong.
     * Nguoc lai, ung vien im lang 15 phut roi dua ra dap an dung thuong bi danh gia thap hon.
     */
    public static List<String> requiredFormat() {
        return List.of(
                "1. CLARIFY (2') - input co rong khong? co am khong? co trung khong? ket qua co can on dinh thu tu?",
                "2. VI DU (1') - tu viet 1 vi du nho va 1 edge case, chay tay",
                "3. NAIVE (2') - noi ra giai phap O(n^2) truoc, DUNG bo qua buoc nay",
                "4. BOTTLENECK (1') - chi ro cho nao cham va VI SAO",
                "5. TOI UU (10') - vua code vua NOI TO tung buoc",
                "6. COMPLEXITY (1') - time va space, giai thich chu khong doc thuoc",
                "7. TEST (3') - tu nghi edge case: rong, 1 phan tu, tat ca giong nhau, gia tri am");
    }
}
