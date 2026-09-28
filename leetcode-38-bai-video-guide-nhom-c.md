# Hướng dẫn tạo Video/GIF minh họa Nhóm C (10 bài HashSet/Dictionary) bằng AI

Tài liệu nguồn: [leetcode-38-bai-phong-van-vietnam.md](leetcode-38-bai-phong-van-vietnam.md#nhom-c). Mục tiêu ở đây
không phải viết lại lời giải, mà biến nội dung **đã có sẵn** (đề bài, ví dụ, hướng giải, code) thành
kịch bản + hình ảnh động cho video/GIF, dùng AI để tăng tốc từng khâu.

Chọn Nhóm C làm thí điểm vì cả 10 bài dùng chung 1 kỹ thuật (HashSet/HashMap) nhưng mỗi bài minh họa
một cách khác nhau — rất hợp để dựng 1 pipeline/template rồi lặp lại cho Nhóm A, B sau.

Video sẽ show **cả code Python và Java** cho mỗi bài — không chỉ Python như bản nháp trước. Bảng dưới
là đường dẫn thật của từng file, dùng để nhúng trực tiếp (không gõ tay lại) khi dựng slide code:

> **Bài 29 đã làm xong, chạy được thật:** [leetcode-38-bai-video/nhom-c/bai-29-contains-duplicate/](leetcode-38-bai-video/nhom-c/bai-29-contains-duplicate/)
> — `bai-29-full.mp4` (1080p30, 2:12, có giọng đọc + phụ đề), `bai-29.gif` (10s, 930 KB).
> Build lại: `./build.sh` (lần đầu tự chạy `../../setup-env.sh`). Sửa lời đọc ở `narration.py`, sửa hình ở `scene.py`.
> **Bài 30 cũng đã xong:** [bai-30-valid-anagram/](leetcode-38-bai-video/nhom-c/bai-30-valid-anagram/) — 2:37, GIF 12s.
> **Bài 31:** [bai-31-isomorphic-strings/](leetcode-38-bai-video/nhom-c/bai-31-isomorphic-strings/) — 3:12, GIF 8s.
> **Bài 32:** [bai-32-longest-consecutive-sequence/](leetcode-38-bai-video/nhom-c/bai-32-longest-consecutive-sequence/) — 3:11, GIF 18s.
> **Bài 33:** [bai-33-subarray-sum-equals-k/](leetcode-38-bai-video/nhom-c/bai-33-subarray-sum-equals-k/) — 3:27.
> **Bài 34:** [bai-34-intersection-of-two-arrays-ii/](leetcode-38-bai-video/nhom-c/bai-34-intersection-of-two-arrays-ii/) — 2:42.
> **Bài 35:** [bai-35-happy-number/](leetcode-38-bai-video/nhom-c/bai-35-happy-number/) — 2:48.
> **Bài 36:** [bai-36-4sum-ii/](leetcode-38-bai-video/nhom-c/bai-36-4sum-ii/) — 2:50.
> **Bài 37:** [bai-37-continuous-subarray-sum/](leetcode-38-bai-video/nhom-c/bai-37-continuous-subarray-sum/) — 3:07.
> **Bài 38:** [bai-38-design-hashmap/](leetcode-38-bai-video/nhom-c/bai-38-design-hashmap/) — 3:32, GIF 15s. **Nhóm C hoàn tất (29–38).**
>
> Làm bài 31–38: phần dùng chung nằm ở `leetcode-38-bai-video/common/` (`kit.py` — màu, ô, khung code,
> giọng + phụ đề; `make_voice.py`; `build.sh`). Mỗi bài chỉ cần 3 file trong `nhom-c/bai-<N>-<tên>/`:
> `narration.py` (`LESSON`, `SEGMENTS`, `TTS_EN`), `scene.py` (lớp `LessonVideo` + `LessonGif`, kế thừa
> `kit.Lesson`), và `build.sh` 1 dòng gọi `../../common/build.sh`.

## 0. Bảng đường dẫn code nguồn (Python + Java) — Nhóm C

| # | Bài | LeetCode | File Python (trong `leetcode-38-bai/`) | File Java (trong `leetcode-38-bai-java/src/main/java/com/motives/leetcode/groupc/`) |
|---|---|---|---|---|
| 29 | Contains Duplicate | #217 | `lc217-contains-duplicate.py` | `ContainsDuplicate.java` |
| 30 | Valid Anagram | #242 | `lc242-valid-anagram.py` | `ValidAnagram.java` |
| 31 | Isomorphic Strings | #205 | `lc205-isomorphic-strings.py` | `IsomorphicStrings.java` |
| 32 | Longest Consecutive Sequence | #128 | `lc128-longest-consecutive-sequence.py` | `LongestConsecutiveSequence.java` |
| 33 | Subarray Sum Equals K | #560 | `lc560-subarray-sum-equals-k.py` | `SubarraySumEqualsK.java` |
| 34 | Intersection of Two Arrays II | #350 | `lc350-intersection-of-two-arrays-ii.py` | `IntersectionOfTwoArrays.java` |
| 35 | Happy Number | #202 | `lc202-happy-number.py` | `HappyNumber.java` |
| 36 | 4Sum II | #454 | `lc454-4sum-ii.py` | `FourSumII.java` |
| 37 | Continuous Subarray Sum | #523 | `lc523-continuous-subarray-sum.py` | `ContinuousSubarraySum.java` |
| 38 | Design HashMap | #706 | `lc706-design-hashmap.py` | `MyHashMap.java` |

Test tương ứng (dùng để tự tin animation không "diễn sai" — chạy pass rồi mới quay) nằm ở
`leetcode-38-bai-java/src/test/java/com/motives/leetcode/groupc/<TênLớp>Test.java` và phần
`if __name__ == "__main__":` cuối mỗi file Python.

---

## 1. Pipeline tổng thể

```
.md có sẵn (đề bài + hướng giải + code)
        │
        ▼
1. Kịch bản (script)      ── LLM (Claude) chuyển nội dung .md thành script có timing
        │
        ▼
2. Storyboard              ── xác định đoạn nào cần animate thuật toán, đoạn nào chỉ cần slide chữ/code
        │
        ▼
3. Animation (Manim)       ── Claude sinh code Manim từ chính hàm Python đã có, không "diễn" tay
        │
        ▼
4. Giọng đọc (TTS)         ── ElevenLabs / OpenAI TTS / Google Cloud TTS, đọc script bước 1
        │
        ▼
5. Ghép (ffmpeg/CapCut)    ── animation + voice + phụ đề + code overlay + nhạc nền
        │
        ▼
6. Export mp4 + gif riêng  ── mp4 full cho YouTube, gif ngắn (đoạn animate) cho LinkedIn/X
```

**Vì sao Manim chứ không phải Canva/CapCut animation dựng tay:** cả 10 bài Nhóm C đều có sẵn code
Python đã chạy được. Manim (Manim Community Edition) cho phép animate **đúng biến, đúng bước lặp**
của thuật toán thật, tránh trường hợp hình vẽ tay diễn sai logic (ví dụ: quên bước "chỉ bắt đầu đếm
dãy nếu num-1 không có trong set" ở bài 32).

Nếu không muốn cài Manim, phương án nhẹ hơn: vẽ tay từng frame bằng Excalidraw/tldraw rồi ghi màn
hình — nhanh hơn nhưng dễ diễn sai bước, chỉ nên dùng cho bài dễ (29, 30).

---

## 2. Cấu trúc thời lượng chuẩn cho 1 video/bài (~4–5 phút, song ngữ Python + Java)

| Đoạn | Thời lượng | Nguồn nội dung (map với .md / file thật) | Hình thức |
|---|---|---|---|
| Hook + đề bài | 10–15s | Mục "Đề bài" + "Ví dụ" | Text/slide, đọc to input/output |
| Animation thuật toán | 60–90s | Mục "Hướng giải quyết", chạy đúng theo code Python | **Manim** — đây là đoạn tách riêng làm GIF |
| Diễn giải hướng giải bằng lời | 30–40s | Nguyên văn mục "Hướng giải quyết" (đã viết sẵn, chỉ cần đọc) | Voice-over + phụ đề |
| Code walkthrough — Python | 30–40s | File `.py` thật trong `leetcode-38-bai/` (xem bảng mục 0) | Code slide, highlight từng dòng theo animation |
| Code walkthrough — Java | 30–40s | File `.java` thật trong `leetcode-38-bai-java/.../groupc/` (xem bảng mục 0) | Code slide, highlight; nói rõ khác biệt so với Python (kiểu tĩnh, `Set<Integer>`, v.v.) |
| Độ phức tạp + tóm tắt | 10–15s | Mục "Độ phức tạp" (dùng chung cho cả 2 ngôn ngữ) | Text/slide |

GIF riêng (15–20s, loop được) = chỉ cắt đúng đoạn "Animation thuật toán", không voice, có thể thêm
caption ngắn 1 dòng.

---

## 3. Trực quan hoá đề xuất cho từng bài Nhóm C

| # | Bài | Kiểu animation | Điểm phải animate đúng (invariant) | Làm GIF riêng? |
|---|---|---|---|---|
| 29 | Contains Duplicate | Dãy ô vuông chứa số, mỗi ô "bay" vào HashSet; ô trùng bật đỏ | Set chỉ tra cứu — không so sánh từng cặp | Có |
| 30 | Valid Anagram | 2 chuỗi song song, mỗi ký tự tăng/giảm 1 thanh histogram (Counter) | Phải giảm ở chuỗi `t`, không phải tạo Counter riêng rồi so `==` một cách "ma thuật" | Có |
| 31 | Isomorphic Strings | 2 hàng ký tự, mũi tên ánh xạ `s→t`, tô đỏ khi vi phạm song ánh | Phải có **2 map hai chiều**, chỉ 1 chiều sẽ minh họa sai | Có |
| 32 | Longest Consecutive Sequence | Các số "rơi" vào set không theo thứ tự; chỉ số nào không có `num-1` mới "nảy" mở rộng sang phải | Bỏ qua bước kiểm tra `num-1` sẽ đếm lại dãy nhiều lần — đây là điểm hay bị hỏi lại khi phỏng vấn | Có (điểm nhấn nhất nhóm) |
| 33 | Subarray Sum Equals K | Thanh trượt tích lũy `prefix_sum` chạy dọc trục số, bảng đếm bên cạnh tăng dần | Map khởi tạo `{0: 1}` — nếu quên, animation phải chỉ rõ vì sao sai ở dãy con bắt đầu từ đầu mảng | Có |
| 34 | Intersection of Two Arrays II | 2 multiset chồng lên nhau, phần giao "rơi" xuống danh sách kết quả | Giữ đúng số lần lặp — khác bài #349 gốc | Không bắt buộc |
| 35 | Happy Number | Đồ thị số → số (biến đổi tổng bình phương chữ số), có node quay vòng (cycle) | Phát hiện chu trình bằng set các số đã thấy, không lặp vô hạn | Có |
| 36 | 4Sum II | 4 mảng, 2 mảng đầu ghép cặp tổng vào map, 2 mảng sau tra ngược | Giảm từ O(n⁴) xuống O(n²) — animate rõ bước "gộp cặp trước" | Không bắt buộc |
| 37 | Continuous Subarray Sum | Giống bài 33 nhưng trục là `prefix_sum % k` | Bẫy: cần khoảng cách ≥ 2 giữa hai chỉ số cùng số dư | Có |
| 38 | Design HashMap | Mảng bucket, mỗi bucket là 1 linked list (chaining khi va chạm) | Đây là bài duy nhất "tự chế" HashMap — animate va chạm bucket | Có |

---

## 4. Prompt mẫu để nhờ Claude sinh code Manim

Dùng lại nguyên văn nội dung từ file gốc, không tự diễn giải lại — tránh AI "bịa" bước sai:

```
Tôi cần code Manim (Community Edition, Python) để minh họa thuật toán sau, dùng cho video LeetCode
tiếng Việt.

Bài: <tên bài + số LeetCode>
Đề bài: <copy nguyên văn mục "Đề bài">
Ví dụ: <copy nguyên văn mục "Ví dụ">
Hướng giải: <copy nguyên văn mục "Hướng giải quyết">
Code Python tham chiếu (animate ĐÚNG theo logic này, không thêm bước nào không có trong code)
— nguồn: leetcode-38-bai/<tên file .py, xem bảng mục 0>:
<dán nguyên nội dung file .py>

Code Java tương đương (dùng cho đoạn code walkthrough Java, KHÔNG animate lại — chỉ cần chỉ ra
chỗ khác biệt so với Python, ví dụ kiểu tĩnh, HashSet<Integer> thay vì set())
— nguồn: leetcode-38-bai-java/src/main/java/com/motives/leetcode/groupc/<tên file .java, xem bảng mục 0>:
<dán nguyên nội dung file .java>

Yêu cầu:
- Scene Manim chỉ animate theo code Python, từng bước vòng lặp thật (không rút gọn logic)
- Thời lượng scene animation 60–90 giây khi render ở tốc độ đọc bình thường
- Dùng self.wait() hợp lý để giọng đọc voice-over kịp theo
- Highlight màu khi phần tử được thêm/tra cứu trong hash set/map
- Sau scene animation, sinh thêm 1 đoạn thoại ngắn (2–3 câu) so sánh Python vs Java cho đúng bài
  này, dùng cho đoạn code walkthrough Java
- Kết thúc scene bằng dòng chữ tóm tắt độ phức tạp: <copy mục "Độ phức tạp">
```

Prompt này dùng được cho cả 9 bài còn lại — chỉ thay các chỗ `<...>` (tên bài, đề bài, ví dụ, hướng
giải, và 2 file Python/Java lấy từ bảng mục 0) bằng nội dung tương ứng đã có sẵn, không cần viết tay
lại.

---

## 5. Ví dụ đầy đủ A→Z: Bài 29 — Contains Duplicate

### 5.1 Script (có timing, đọc trực tiếp khi thu voice)

```
[0:00–0:12] (Hook + đề bài)
"Cho một mảng số nguyên, làm sao biết mảng có phần tử trùng lặp hay không — nhanh nhất có thể?"
Input: nums = [1, 2, 3, 1] → Output: true

[0:12–1:30] (Animation — đoạn cắt GIF)
Duyệt từng số trong mảng. Với mỗi số, kiểm tra: đã có trong tập đã thấy chưa?
- Chưa có → thêm vào tập, đi tiếp.
- Đã có → dừng lại, trả về true ngay lập tức.

[1:30–2:10] (Diễn giải)
"Đây là ví dụ cơ bản nhất cho việc dùng HashSet để tra cứu tồn tại trong O(1),
thay vì so sánh từng cặp phần tử với nhau tốn O(n bình phương)."

[2:10–2:45] (Code walkthrough — Python, nguồn: leetcode-38-bai/lc217-contains-duplicate.py)
class Solution:
    def contains_duplicate(self, nums: list[int]) -> bool:
        seen = set()
        for num in nums:
            if num in seen:
                return True
            seen.add(num)
        return False

[2:45–3:20] (Code walkthrough — Java, nguồn: leetcode-38-bai-java/.../groupc/ContainsDuplicate.java)
public class ContainsDuplicate {
    public boolean containsDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int num : nums) {
            if (!seen.add(num)) {
                return true;
            }
        }
        return false;
    }
}
"Cùng logic, nhưng Java gộp bước kiểm tra và thêm vào set làm một, nhờ Set.add() trả về false
khi phần tử đã tồn tại."

[3:20–3:35] (Tóm tắt)
"Time O(n), Space O(n) — ở cả 2 ngôn ngữ."
```

### 5.2 Manim scene khung sườn (animate đúng theo code Python đã test)

```python
from manim import *


class ContainsDuplicate(Scene):
    def construct(self):
        nums = [1, 2, 3, 1]

        title = Text("Contains Duplicate — LeetCode #217", font_size=32)
        self.play(Write(title))
        self.play(title.animate.to_edge(UP))

        cells = VGroup(*[
            Square(side_length=1).set_stroke(WHITE)
            for _ in nums
        ]).arrange(RIGHT, buff=0.3)
        labels = VGroup(*[
            Text(str(n), font_size=28).move_to(cell)
            for n, cell in zip(nums, cells)
        ])
        self.play(FadeIn(cells), FadeIn(labels))

        seen_box = Rectangle(width=3, height=1.5).to_edge(DOWN)
        seen_label = Text("seen = {}", font_size=24).move_to(seen_box)
        self.play(FadeIn(seen_box), FadeIn(seen_label))

        seen = set()
        for i, num in enumerate(nums):
            self.play(Indicate(cells[i], color=YELLOW))
            if num in seen:
                self.play(cells[i].animate.set_fill(RED, opacity=0.6))
                result = Text("→ True (trùng lặp)", color=RED, font_size=28)
                result.next_to(seen_box, UP)
                self.play(Write(result))
                break
            seen.add(num)
            new_label = Text(f"seen = {seen}", font_size=24).move_to(seen_box)
            self.play(cells[i].animate.set_fill(GREEN, opacity=0.4))
            self.play(Transform(seen_label, new_label))

        self.wait(2)
```

Render thử:

```bash
pip install manim
manim -pql contains_duplicate.py ContainsDuplicate   # -pql = preview, quality thấp để duyệt nhanh
manim -pqh contains_duplicate.py ContainsDuplicate   # -pqh = bản chất lượng cao để xuất thật
```

Scene này **chỉ animate phần thuật toán** (đúng logic Python). Đoạn "Code walkthrough" cho cả 2
ngôn ngữ ở 2:10–3:20 không cần animate lại bằng Manim — dựng đơn giản hơn bằng 1 trong 2 cách:

- **Nhanh:** chụp code trực tiếp từ file thật bằng công cụ screenshot đẹp (Carbon, Silicon, hoặc
  extension "Code Snap" trong VS Code/Cursor), ghép vào timeline ở bước 5 (ffmpeg/CapCut).
- **Đồng bộ với animation:** thêm 1 `Scene` Manim khác dùng `Code` mobject
  (`from manim import Code`) load trực tiếp file `.py`/`.java` bằng đường dẫn thật, tránh gõ lại tay:

```python
from manim import Code, Scene

class CodeWalkthroughPython(Scene):
    def construct(self):
        code = Code(
            "leetcode-38-bai/lc217-contains-duplicate.py",
            tab_width=4, language="python", font="Monospace",
        )
        self.play(Create(code))
        self.wait(3)


class CodeWalkthroughJava(Scene):
    def construct(self):
        code = Code(
            "leetcode-38-bai-java/src/main/java/com/motives/leetcode/groupc/ContainsDuplicate.java",
            tab_width=4, language="java", font="Monospace",
        )
        self.play(Create(code))
        self.wait(3)
```

Cách này đảm bảo code hiển thị trên video **luôn khớp với file thật** — sửa code nguồn thì lần
render sau tự cập nhật theo, không phải sửa tay lại trong script Manim.

### 5.3 Ghép voice + xuất mp4/gif bằng ffmpeg

```bash
# Ghép video Manim với file voice-over đã thu (ví dụ voice.mp3)
ffmpeg -i ContainsDuplicate.mp4 -i voice.mp3 -c:v copy -c:a aac -shortest bai-29-full.mp4

# Cắt riêng đoạn animation (giây 12 -> 90) để làm GIF loop cho LinkedIn/X
ffmpeg -i ContainsDuplicate.mp4 -ss 00:00:12 -to 00:00:90 -vf "fps=15,scale=480:-1:flags=lanczos" bai-29.gif
```

### 5.4 Gợi ý cấu trúc thư mục lưu trữ

```
algorithms-prep/
  leetcode-38-bai-video/
    nhom-c/
      bai-29-contains-duplicate/
        script.md
        scene.py          # Manim source
        voice.mp3
        bai-29-full.mp4
        bai-29.gif
```

---

## 6. Quy trình lặp cho 9 bài còn lại của Nhóm C

1. Copy 4 mục cần thiết (đề bài, ví dụ, hướng giải, code) từ file gốc — không viết tay lại.
2. Điền vào prompt mẫu ở mục 4, đổi phần "Yêu cầu" animate theo đúng cột "Kiểu animation" ở bảng
   mục 3.
3. Render thử bản `-pql` (nhanh, chất lượng thấp) để duyệt logic animate có đúng không, đối chiếu
   từng bước với code Python đã chạy trong `leetcode-38-bai/`.
4. Chỉ khi animate đúng mới thu voice + render bản `-pqh` + export gif.
5. Lặp lại cho từng bài, giữ nguyên cấu trúc thư mục ở mục 5.4 (đổi `bai-29-...` thành số bài tương
   ứng).

---

## 7. Checklist trước khi publish

- [ ] Animation khớp 100% với code Python đã test trong `leetcode-38-bai/` — không có bước "diễn"
      thêm hoặc bỏ bớt so với code thật.
- [ ] Code walkthrough Java lấy đúng từ file trong `leetcode-38-bai-java/.../groupc/` (bảng mục 0),
      không viết tay lại; nếu file Java đã sửa sau khi quay, phải re-render lại đoạn code slide.
- [ ] Với bài 32 và 37 (2 bài có "bẫy" logic — điều kiện bắt đầu dãy, khoảng cách ≥ 2): animation
      phải làm nổi bật rõ ràng phần bẫy đó, vì đây là chỗ hay bị hỏi lại khi phỏng vấn.
- [ ] Phụ đề tiếng Việt không lỗi dấu, không lỗi font khi render.
- [ ] Thời lượng đúng định dạng nền tảng: YouTube Shorts/TikTok ≤ 60s (dùng bản rút gọn chỉ có
      animation + voice ngắn), video đầy đủ lên YouTube thường 3–4 phút, GIF loop ≤ 15–20s cho
      LinkedIn/X.
- [ ] Xóa watermark công cụ dùng thử (nếu có) trước khi đăng công khai.

---

## Ranh giới trung thực

- **Đã chạy thật (bài 29, 2026-09-27):** manim 0.21.0 + ffmpeg 7.1 (từ `imageio-ffmpeg`) trên macOS arm64;
  `setup-env.sh` và `build.sh` chạy trọn từ đầu, build mất ~66s. Hai bẫy khi cài: pycairo cần
  `pkg-config` và file `.pc` của zlib/bzip2/expat — `setup-env.sh` đã xử lý. API `Code` của manim 0.21
  khác các ví dụ cũ ở mục 5.2: tham số là `code_string`/`code_file`, `paragraph_config={"font": ...}`
  (không còn `font=` trực tiếp) — code thật xem `scene.py`.
- **Bẫy đã gặp — manim làm rơi tiếng khi dùng cache:** animation lấy từ cache bật `skip_animations`, và
  `add_sound()` gọi ngay sau đó bị bỏ qua **không báo lỗi**. Bài 29 từng mất 9/22 đoạn (các đoạn mở đầu
  ngay sau một hiệu ứng chuyển cảnh, như đề bài). `build.sh` giờ luôn render với `--disable_caching`.
  Kiểm tra: dò từng `audio/<đoạn>.wav` trong audio của mp4 bằng tương quan — cả 22 đoạn khớp 0,98–0,99.
- **Giọng đọc:** mặc định là `vi-VN-NamMinhNeural` (nam, neural, qua `edge-tts`, không cần key);
  đổi bằng `VOICE=linh|hoaimy|file TEXT=phienam|tienganh ./build.sh`, nghe thử mẫu ở
  `bai-29-contains-duplicate/audio/samples/`. `edge-tts` dùng endpoint "Read aloud" của Edge — **không phải
  API chính thức**, hay bị chặn tạm khi gọi dồn dập (đã có retry + nghỉ giữa các đoạn; bài 29 mất ~4 phút cho
  22 đoạn). Nếu đăng kênh công khai/thương mại, chuyển sang Azure Speech chính thức (cùng giọng HoaiMy/NamMinh,
  có free tier) hoặc ElevenLabs — chỉ cần thêm 1 hàm `synth_*` trong `make_voice.py`, timing tự khớp theo
  `durations.json`. **Chưa kiểm chứng:** tôi không nghe được audio — chất lượng giọng do bạn đánh giá.

- **Đã kiểm chứng:** cấu trúc nội dung/timing dựa trực tiếp trên các mục có sẵn và đã chạy được
  trong [leetcode-38-bai-phong-van-vietnam.md](leetcode-38-bai-phong-van-vietnam.md); bảng đường
  dẫn ở mục 0 và 2 đoạn code Python/Java ở mục 5.1 (bài 29) đã đối chiếu trực tiếp với file thật
  trong `leetcode-38-bai/lc217-contains-duplicate.py` và
  `leetcode-38-bai-java/.../groupc/ContainsDuplicate.java` — khớp 100%, không có 9 file Python và
  9 file Java còn lại nào bị đổi tên khác với bảng mục 0 tại thời điểm viết tài liệu này.
- **Chưa kiểm chứng (suy luận từ tài liệu Manim/ffmpeg công khai):** đoạn code Manim ở mục 5.2
  (bao gồm cả 2 scene `CodeWalkthroughPython`/`CodeWalkthroughJava` dùng `Code` mobject) và các
  lệnh `ffmpeg`/`manim` ở mục 5.3, 6 — **chưa được chạy thử trong workspace này**. Riêng `Code`
  mobject của Manim có thay đổi API khá nhiều giữa các phiên bản gần đây — kiểm tra kỹ tham số
  (`tab_width`, `language`, `font`) khớp với bản Manim đang cài trước khi lặp cho cả 10 bài. Trước
  khi dựng hàng loạt, nên chạy thử bài 29 (Manim + ffmpeg, cả 2 ngôn ngữ) một lần để xác nhận cú
  pháp, rồi mới lặp lại cho 9 bài còn lại.
