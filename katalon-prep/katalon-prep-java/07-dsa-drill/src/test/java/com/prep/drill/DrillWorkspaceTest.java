package com.prep.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.drill.DrillWorkspace.TreeNode;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ============================================================================
 * BO @Disabled O NHOM NAO BAN DANG LUYEN.
 * ============================================================================
 *
 * <p>Mac dinh tat ca deu {@code @Disabled} de {@code mvn test} o thu muc goc van xanh. Khi bat dau
 * luyen mot bai: xoa dong {@code @Disabled} cua nhom do, bam gio, viet trong
 * {@link DrillWorkspace}, chay {@code mvn -pl 07-dsa-drill test}.
 *
 * <p>Test o day co CA edge case — vi tu nghi ra edge case chinh la buoc 7 trong format bat buoc,
 * va la thu interviewer luon hoi ("test case nao ban se viet?"). Doc truoc phan edge case cua tung
 * bai truoc khi giai cung duoc — muc tieu la tao phan xa, khong phai danh do.
 */
class DrillWorkspaceTest {

    @Nested
    @Disabled("BO DONG NAY khi bat dau luyen #1 Two Sum (muc tieu: 10 phut)")
    @DisplayName("#1 Two Sum")
    class TwoSum {

        @Test
        @DisplayName("truong hop co ban")
        void basic() {
            assertArrayEqualsInAnyOrder(new int[] {0, 1}, DrillWorkspace.twoSum(new int[] {2, 7, 11, 15}, 9));
        }

        @Test
        @DisplayName("EDGE: so am")
        void negatives() {
            assertArrayEqualsInAnyOrder(new int[] {0, 2}, DrillWorkspace.twoSum(new int[] {-3, 4, 3}, 0));
        }

        @Test
        @DisplayName("EDGE: gia tri trung nhau")
        void duplicates() {
            assertArrayEqualsInAnyOrder(new int[] {0, 1}, DrillWorkspace.twoSum(new int[] {3, 3}, 6));
        }

        @Test
        @DisplayName("EDGE: khong co dap an -> mang rong, KHONG nem")
        void noSolution() {
            assertEquals(0, DrillWorkspace.twoSum(new int[] {1, 2}, 100).length);
        }

        @Test
        @DisplayName("EDGE: mang rong")
        void empty() {
            assertEquals(0, DrillWorkspace.twoSum(new int[] {}, 0).length);
        }

        private static void assertArrayEqualsInAnyOrder(int[] expected, int[] actual) {
            int[] a = expected.clone();
            int[] b = actual.clone();
            Arrays.sort(a);
            Arrays.sort(b);
            assertEquals(Arrays.toString(a), Arrays.toString(b));
        }
    }

    @Nested
    @Disabled("BO DONG NAY khi bat dau luyen #102 Level Order Traversal (20 phut)")
    @DisplayName("#102 Binary Tree Level Order Traversal")
    class LevelOrder {

        @Test
        @DisplayName("cay 3 tang")
        void threeLevels() {
            var root = new TreeNode(3, new TreeNode(9), new TreeNode(20, new TreeNode(15), new TreeNode(7)));
            assertEquals(
                    List.of(List.of(3), List.of(9, 20), List.of(15, 7)), DrillWorkspace.levelOrder(root));
        }

        @Test
        @DisplayName("EDGE: cay rong -> list rong, KHONG nem NPE")
        void emptyTree() {
            assertEquals(List.of(), DrillWorkspace.levelOrder(null));
        }

        @Test
        @DisplayName("EDGE: cay lech hoan toan mot ben (moi tang 1 node)")
        void skewedTree() {
            var root = new TreeNode(1, new TreeNode(2, new TreeNode(3), null), null);
            assertEquals(List.of(List.of(1), List.of(2), List.of(3)), DrillWorkspace.levelOrder(root));
        }
    }

    @Nested
    @Disabled("BO DONG NAY khi bat dau luyen #207 Course Schedule (25 phut)")
    @DisplayName("#207 Course Schedule - topological sort")
    class CourseSchedule {

        @Test
        @DisplayName("khong co chu trinh -> true")
        void acyclic() {
            assertTrue(DrillWorkspace.canFinish(2, new int[][] {{1, 0}}));
        }

        @Test
        @DisplayName("co chu trinh -> false")
        void cyclic() {
            assertFalse(DrillWorkspace.canFinish(2, new int[][] {{1, 0}, {0, 1}}));
        }

        @Test
        @DisplayName("EDGE: khong co rang buoc nao -> true")
        void noPrerequisites() {
            assertTrue(DrillWorkspace.canFinish(5, new int[][] {}));
        }

        @Test
        @DisplayName("EDGE: chu trinh dai (3 node)")
        void longCycle() {
            assertFalse(DrillWorkspace.canFinish(3, new int[][] {{1, 0}, {2, 1}, {0, 2}}));
        }

        @Test
        @DisplayName("EDGE: mot mon phu thuoc chinh no")
        void selfLoop() {
            assertFalse(DrillWorkspace.canFinish(1, new int[][] {{0, 0}}));
        }
    }

    @Nested
    @Disabled("BO DONG NAY khi bat dau luyen #253 Meeting Rooms II (25 phut)")
    @DisplayName("#253 Meeting Rooms II - scheduling test vao executor pool")
    class MeetingRooms {

        @Test
        @DisplayName("chong lan -> can 2 phong")
        void overlapping() {
            assertEquals(2, DrillWorkspace.minMeetingRooms(new int[][] {{0, 30}, {5, 10}, {15, 20}}));
        }

        @Test
        @DisplayName("khong chong lan -> 1 phong")
        void sequential() {
            assertEquals(1, DrillWorkspace.minMeetingRooms(new int[][] {{7, 10}, {2, 4}}));
        }

        @Test
        @DisplayName("EDGE: rong -> 0")
        void empty() {
            assertEquals(0, DrillWorkspace.minMeetingRooms(new int[][] {}));
        }

        @Test
        @DisplayName("EDGE: ket thuc trung dung luc bat dau -> VAN 1 phong (khong tinh la chong lan)")
        void touchingIntervals() {
            assertEquals(1, DrillWorkspace.minMeetingRooms(new int[][] {{1, 5}, {5, 10}}));
        }

        @Test
        @DisplayName("EDGE: tat ca chong lan hoan toan -> n phong")
        void allOverlap() {
            assertEquals(3, DrillWorkspace.minMeetingRooms(new int[][] {{1, 10}, {2, 10}, {3, 10}}));
        }
    }
}
