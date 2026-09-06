"""Module 07 — cho luyen tay. **TAT COPILOT TRUOC KHI MO FILE NAY.**

Doi xung voi `07-dsa-drill` ben Java: cung 4 bai, cung format 7 buoc. Muc dich khong
phai hoc thuat toan moi (ban da co 38 bai o `leetcode-38-bai/`), ma la **giu duoc phan
xa o CA HAI ngon ngu** — vi neu vong coding cho chon ngon ngu, ban muon chon duoc bang
nang luc chu khong bang bat dac di.

FORMAT BAT BUOC (giong ben Java, va giong thu interviewer muon nghe):
  1. Nhac lai de bang loi cua minh
  2. Hoi ro rang buoc: kich thuoc? co am khong? co trung khong? rong thi sao?
  3. Neu vi du bang tay
  4. Noi cach lam THO truoc, kem do phuc tap
  5. Noi cach lam TOI UU, kem do phuc tap, va vi sao tot hon
  6. Moi viet code
  7. Tu neu edge case va tu chay thu bang tay

Buoc 2 va 7 la thu phan biet ung vien Senior/Lead voi ung vien moi. Bo qua chung thi
du code dung van bi danh gia thap.

CACH DUNG: bo `@pytest.mark.skip` cua nhom tuong ung trong `tests/test_07_dsa.py`,
bam gio, viet vao day.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class TreeNode:
    val: int
    left: TreeNode | None = None
    right: TreeNode | None = None


def two_sum(nums: list[int], target: int) -> list[int]:
    """#1 — muc tieu 10 phut.

    Rang buoc phai TU HOI truoc khi code: co so am khong? co gia tri trung khong?
    khong co dap an thi tra gi (rong hay nem)? mang rong thi sao?
    """
    raise NotImplementedError("viet o day - tat Copilot")


def level_order(root: TreeNode | None) -> list[list[int]]:
    """#102 — muc tieu 20 phut. BFS theo tang.

    Lien he Katalon: cay test suite -> test case, duyet theo tang de bao cao tien do.
    """
    raise NotImplementedError("viet o day - tat Copilot")


def can_finish(num_courses: int, prerequisites: list[list[int]]) -> bool:
    """#207 — muc tieu 25 phut. Topological sort / phat hien chu trinh.

    Lien he Katalon TRUC TIEP: test case co phu thuoc lan nhau (test B can du lieu do
    test A tao ra). Co chu trinh = khong the xep lich chay = phai bao loi cho nguoi dung
    thay vi treo vinh vien. Day la bai dang chuan bi ky nhat trong 4 bai.
    """
    raise NotImplementedError("viet o day - tat Copilot")


def min_meeting_rooms(intervals: list[list[int]]) -> int:
    """#253 — muc tieu 25 phut.

    Lien he Katalon: so executor toi thieu de chay het cac test co rang buoc thoi gian.
    Chinh la bai toan o `08-system-design` ben Java, nhung o dang toi gian.
    """
    raise NotImplementedError("viet o day - tat Copilot")
