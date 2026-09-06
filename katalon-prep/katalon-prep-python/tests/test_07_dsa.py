"""Module 07 — BO `@pytest.mark.skip` cua nhom nao ban dang luyen.

Mac dinh tat ca deu skip de `uv run pytest` van xanh. Test o day co CA edge case, vi
tu nghi ra edge case chinh la buoc 7 trong format bat buoc — va la thu interviewer luon
hoi ("ban se viet test case nao?").

Doc truoc phan edge case roi moi giai cung duoc: muc tieu la tao phan xa, khong phai danh do.
"""

from __future__ import annotations

import pytest

from prep.dsa.workspace import TreeNode, can_finish, level_order, min_meeting_rooms, two_sum


@pytest.mark.skip(reason="BO DONG NAY khi bat dau luyen #1 Two Sum (muc tieu 10 phut)")
class TestTwoSum:
    def test_co_ban(self):
        assert sorted(two_sum([2, 7, 11, 15], 9)) == [0, 1]

    def test_EDGE_so_am(self):
        assert sorted(two_sum([-3, 4, 3], 0)) == [0, 2]

    def test_EDGE_gia_tri_trung_nhau(self):
        assert sorted(two_sum([3, 3], 6)) == [0, 1]

    def test_EDGE_khong_co_dap_an_tra_RONG_khong_NEM(self):
        assert two_sum([1, 2], 100) == []

    def test_EDGE_mang_rong(self):
        assert two_sum([], 0) == []


@pytest.mark.skip(reason="BO DONG NAY khi bat dau luyen #102 Level Order (20 phut)")
class TestLevelOrder:
    def test_cay_3_tang(self):
        root = TreeNode(3, TreeNode(9), TreeNode(20, TreeNode(15), TreeNode(7)))
        assert level_order(root) == [[3], [9, 20], [15, 7]]

    def test_EDGE_cay_rong_tra_list_rong_khong_NEM(self):
        assert level_order(None) == []

    def test_EDGE_cay_lech_han_mot_ben(self):
        root = TreeNode(1, TreeNode(2, TreeNode(3)))
        assert level_order(root) == [[1], [2], [3]]


@pytest.mark.skip(reason="BO DONG NAY khi bat dau luyen #207 Course Schedule (25 phut)")
class TestCourseSchedule:
    def test_khong_chu_trinh(self):
        assert can_finish(2, [[1, 0]]) is True

    def test_co_chu_trinh(self):
        assert can_finish(2, [[1, 0], [0, 1]]) is False

    def test_EDGE_khong_rang_buoc_nao(self):
        assert can_finish(5, []) is True

    def test_EDGE_chu_trinh_dai_3_node(self):
        assert can_finish(3, [[1, 0], [2, 1], [0, 2]]) is False

    def test_EDGE_mot_mon_phu_thuoc_chinh_no(self):
        assert can_finish(1, [[0, 0]]) is False


@pytest.mark.skip(reason="BO DONG NAY khi bat dau luyen #253 Meeting Rooms II (25 phut)")
class TestMeetingRooms:
    def test_chong_lan_can_2_phong(self):
        assert min_meeting_rooms([[0, 30], [5, 10], [15, 20]]) == 2

    def test_khong_chong_lan(self):
        assert min_meeting_rooms([[7, 10], [2, 4]]) == 1

    def test_EDGE_rong(self):
        assert min_meeting_rooms([]) == 0

    def test_EDGE_ket_thuc_TRUNG_luc_bat_dau_van_1_phong(self):
        assert min_meeting_rooms([[1, 5], [5, 10]]) == 1

    def test_EDGE_tat_ca_chong_lan_hoan_toan(self):
        assert min_meeting_rooms([[1, 10], [2, 10], [3, 10]]) == 3


def test_workspace_con_nguyen_ban_stub():
    """Test duy nhat CHAY - de nhac rang cac nhom tren dang bi skip."""
    with pytest.raises(NotImplementedError):
        two_sum([1, 2], 3)
