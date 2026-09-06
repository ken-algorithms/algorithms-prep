package com.prep.drill;

import java.util.List;

/**
 * ============================================================================
 * VIET LOI GIAI CUA BAN O DAY. TAT COPILOT TRUOC.
 * ============================================================================
 *
 * <p>Moi method duoi day dang nem {@link UnsupportedOperationException}. Test tuong ung o
 * {@code DrillWorkspaceTest} se DO cho den khi ban tu viet.
 *
 * <p>Quy tac:
 * <ul>
 *   <li>Bam gio theo {@code Drill.Problem.minutes()}.
 *   <li>KHONG mo file .md truoc khi het gio.
 *   <li>NOI TO trong luc viet — day la ky nang duoc cham, khong phai toc do go.
 *   <li>Sau khi test xanh, doi chieu voi ban trong {@code leetcode-38-bai-phong-van-vietnam.md}
 *       va ghi lai cho nao ban viet khac/kem hon.
 * </ul>
 *
 * <p>Bat dau bang 4 bai nay (tuan 1). Khi lam quen roi, tu them method cho cac bai con lai trong
 * {@link Drill#onDomainSubset()}.
 */
public final class DrillWorkspace {

    private DrillWorkspace() {}

    /**
     * LeetCode #1 — Two Sum. Muc tieu: duoi 10 phut, khong tra cuu.
     *
     * @return chi so cua hai phan tu co tong bang target, hoac mang rong neu khong co
     */
    public static int[] twoSum(int[] nums, int target) {
        throw new UnsupportedOperationException("TODO: viet tay, tat Copilot");
    }

    /**
     * LeetCode #102 — Binary Tree Level Order Traversal.
     * Lien he Katalon: duyet DOM tree theo tang.
     */
    public static List<List<Integer>> levelOrder(TreeNode root) {
        throw new UnsupportedOperationException("TODO: viet tay, tat Copilot");
    }

    /**
     * LeetCode #207 — Course Schedule.
     * Lien he Katalon: THU TU PHU THUOC GIUA TEST — co chay het duoc khong, hay co vong lap?
     *
     * @param prerequisites moi phan tu la {@code [course, prerequisite]}
     * @return true neu KHONG co chu trinh
     */
    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        throw new UnsupportedOperationException("TODO: viet tay, tat Copilot");
    }

    /**
     * LeetCode #253 — Meeting Rooms II.
     * Lien he Katalon: so executor toi thieu de chay het test song song ma khong trung gio.
     * Khi giai xong, TU NOI RA lien he nay — do la thu interviewer nho.
     *
     * @param intervals moi phan tu la {@code [start, end]}
     * @return so phong (executor) toi thieu
     */
    public static int minMeetingRooms(int[][] intervals) {
        throw new UnsupportedOperationException("TODO: viet tay, tat Copilot");
    }

    /** Node cay dung cho bai #102. Giu don gian, khong phai record vi can null-able children. */
    public static final class TreeNode {
        public final int val;
        public final TreeNode left;
        public final TreeNode right;

        public TreeNode(int val) {
            this(val, null, null);
        }

        public TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }
}
