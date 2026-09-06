# Migrate LLM không được phép sai: bảo toàn kết quả, đo được, và scale

> **Vì sao file này tồn tại:** bạn đã trượt đúng ở nhóm kỹ năng này. Yêu cầu của họ không phải
> *"biết self-host"* mà là bốn thứ khó hơn hẳn:
>
> 1. Chuyển từ gọi OpenAI sang self-host **mà hệ đang chạy không bị ảnh hưởng**
> 2. **Chứng minh** kết quả được bảo toàn — không phải tin, mà đo
> 3. **Test tự động và so sánh được** trên một hệ thống có đầu ra không tất định
> 4. **Scale** và **không mất request** khi nhiều người dùng gọi cùng lúc
>
> Đây là tài liệu để **học**, không phải để thuộc. Mỗi mục có công thức, ví dụ tính bằng số, và
> việc cần tự làm. Bổ sung cho [katalon-selfhost-llm.md](katalon-selfhost-llm.md) — file đó là
> *câu chuyện*, file này là *kỹ năng*.

---

## Mục lục

- [1. Sai lầm gốc: "accuracy không đổi" không chứng minh gì](#1-sai-lầm-gốc-accuracy-không-đổi-không-chứng-minh-gì)
- [2. Vì sao không thể diff đầu ra, kể cả ở temperature 0](#2-vì-sao-không-thể-diff-đầu-ra-kể-cả-ở-temperature-0)
- [3. Cần bao nhiêu mẫu — công thức và ví dụ](#3-cần-bao-nhiêu-mẫu--công-thức-và-ví-dụ)
- [4. Eval harness: đo cái gì, ở tầng nào](#4-eval-harness-đo-cái-gì-ở-tầng-nào)
- [5. Năm giai đoạn migrate không ảnh hưởng hệ đang chạy](#5-năm-giai-đoạn-migrate-không-ảnh-hưởng-hệ-đang-chạy)
- [6. CI gate cho hệ không tất định](#6-ci-gate-cho-hệ-không-tất-định)
- [7. Capacity: tính từ nguyên lý, KV cache mới là trần thật](#7-capacity-tính-từ-nguyên-lý-kv-cache-mới-là-trần-thật)
- [8. Không mất request khi tải cao](#8-không-mất-request-khi-tải-cao)
- [9. Scale ngang: nhiều replica, routing, autoscale GPU](#9-scale-ngang-nhiều-replica-routing-autoscale-gpu)
- [10. Kịch bản trả lời + 12 follow-up](#10-kịch-bản-trả-lời--12-follow-up)
- [11. Lộ trình học 4 tuần — có số đo](#11-lộ-trình-học-4-tuần--có-số-đo)
- [12. Ranh giới trung thực](#12-ranh-giới-trung-thực)

---

## 1. Sai lầm gốc: "accuracy không đổi" không chứng minh gì

Đây là ý quan trọng nhất của cả file. Nếu chỉ nhớ được một điều, nhớ điều này.

```text
Bạn chạy golden set 900 câu trên cloud:      accuracy 84,1%
Chuyển sang self-host, chạy lại:             accuracy 84,3%
Kết luận thường gặp:  "không đổi, chuyển được"

SAI. Con số tổng CHE MẤT điều đang thực sự xảy ra bên dưới:

     100 câu TỐT LÊN   ─┐
                        ├──►  chênh lệch tổng = +0,2 điểm
      80 câu XẤU ĐI    ─┘

     → 80 khách hàng nhận kết quả TỆ HƠN TRƯỚC, và bảng số của bạn nói "không đổi".
```

**Đây không phải lo xa lý thuyết.** Một nghiên cứu 2026 đo 900 item × 50 lần chạy trên ba lần nâng
model thương mại tìm thấy: *"Một mức tăng ròng 2 điểm phần trăm có thể gồm 100 câu trả lời đáng tin
hơn và 80 câu kém tin hơn; con số tổng chỉ báo cáo phần chênh lệch ròng."* Cụ thể:

| Migration | Chênh lệch **tổng** | Nhưng bên dưới |
|---|---|---|
| Omni-MATH hard | **+7,3 điểm** | vẫn có **6,0% số câu tụt hẳn** |
| IFBench | **−3,9 điểm** | vẫn có **6,7% số câu tốt hẳn lên** |

Và điều đáng sợ nhất: **cả 9/9 tổ hợp migration-benchmark đều có cả cải thiện lẫn tụt hạng cùng
tồn tại.** Không có lần nào "chỉ tốt lên".

### Phải đo gì thay cho accuracy tổng

| Chỉ số | Nghĩa là gì |
|---|---|
| **P⁺ — tỉ lệ cải thiện đáng tin** | % item tốt lên **có ý nghĩa thống kê VÀ đủ lớn để đáng kể** |
| **P⁻ — tỉ lệ tụt đáng tin** | % item xấu đi theo cùng tiêu chuẩn ← **đây là con số quyết định go/no-go** |
| **Tương đương thực dụng** | % item thay đổi không đáng kể |
| **Chưa kết luận được** | % item chưa đủ bằng chứng — **phải báo cáo, không được giấu** |

**Cách phân loại** (từ chính nghiên cứu trên, và đây là mức chặt chẽ họ mong đợi ở Principal):

1. Chạy **mỗi item K lần** (họ dùng K=50) để ước lượng *xác suất đúng*, không phải một kết quả đơn lẻ
2. **Fisher exact test** so hai tỉ lệ trên từng item
3. **Benjamini-Hochberg FDR ở 5%** — vì test 900 item cùng lúc thì kiểu gì cũng có cái "có ý nghĩa"
   do ngẫu nhiên. Không hiệu chỉnh đa kiểm định là sai
4. Cộng thêm **ngưỡng độ lớn tối thiểu** (họ dùng 20 điểm phần trăm) — có ý nghĩa thống kê chưa đủ,
   phải **đáng kể trên thực tế**
5. **Hiệu chỉnh bằng permutation null**: trộn ngẫu nhiên nhãn "cloud" và "self-host" rồi chạy lại
   toàn bộ quy trình. Nếu quy trình của bạn vẫn tìm ra "thay đổi đáng tin" trên dữ liệu đã trộn thì
   nó đang tạo ra kết quả giả

> **Bước 5 là bước phân biệt người biết làm với người đọc blog.** Nó trả lời câu hỏi *"làm sao bạn
> biết cái bạn đo là tín hiệu chứ không phải nhiễu?"* — và trong nghiên cứu trên, permutation null
> cho **0 thay đổi giả qua 1.000 lần lặp**, tức quy trình của họ đủ chặt.

**Câu nói ra khi được hỏi "làm sao đảm bảo kết quả không đổi":**

> *"Tôi sẽ không dùng accuracy tổng làm bằng chứng, vì nó che mất thay đổi ở tầng từng item — một
> mức tăng ròng 2 điểm có thể là 100 câu tốt lên và 80 câu xấu đi. Cái tôi đo là **tỉ lệ item tụt
> hạng đáng tin**: chạy mỗi item nhiều lần, so bằng Fisher exact có hiệu chỉnh FDR, cộng ngưỡng độ
> lớn tối thiểu, rồi hiệu chỉnh bằng permutation null để chắc mình không đang đo nhiễu. Cổng go/no-go
> của tôi đặt trên P⁻ chứ không đặt trên điểm trung bình."*

---

## 2. Vì sao không thể diff đầu ra, kể cả ở temperature 0

Phản xạ đầu tiên của kỹ sư là: *đặt `temperature=0`, chạy hai bên, so chuỗi.* Cách này **không chạy
được**, và biết vì sao là một câu trả lời ăn điểm.

### Ba tầng bất định, xếp theo mức khó chịu

| Tầng | Nguyên nhân | Có tắt được không |
|---|---|---|
| **1. Sampling** | `temperature > 0`, top-p, top-k | ✅ Tắt được bằng `temperature=0` |
| **2. Số học dấu phẩy động theo batch** | Batch khác nhau → **thứ tự cộng dồn khác** → kết quả float khác ở chữ số cuối → **đôi khi lật argmax** | ❌ **Không tắt được** khi còn continuous batching |
| **3. Khác nhau giữa hai backend** | Kernel khác, dtype khác (FP8 vs BF16), thứ tự reduce khác | ❌ Không |

**Tầng 2 là tầng người ta không biết.** Cùng một prompt, cùng `temperature=0`, gửi hai lần vào vLLM
lúc tải khác nhau → có thể ra hai chuỗi khác nhau. Không phải bug: cộng dấu phẩy động **không có
tính kết hợp**, `(a+b)+c ≠ a+(b+c)`, mà continuous batching thay đổi việc ai được cộng chung với ai.

```text
Hệ quả trực tiếp:
  - Test "output phải khớp chuỗi này" sẽ FLAKY, và bạn sẽ đổ lỗi nhầm cho model
  - Chạy MỘT lần rồi kết luận là vô nghĩa — phải chạy K lần và so PHÂN PHỐI
  - Đó là lý do §1 nói "ước lượng xác suất đúng", không nói "kết quả đúng hay sai"
```

### So cái gì thay cho so chuỗi — bốn tầng, từ chặt xuống lỏng

| Tầng | So cái gì | Dùng khi | Chi phí |
|:---:|---|---|---|
| **1** | **Trường có cấu trúc**: JSON parse ra rồi so từng field | Đầu ra có schema — **phần lớn hệ production** | Rẻ, xác định, không cần LLM |
| **2** | **Bất biến / property**: schema hợp lệ, mã hàng nằm trong danh mục, tổng nợ = tổng có | Luôn luôn nên có | Rẻ |
| **3** | **Chỉ số theo tác vụ**: exact match, F1 theo field, Recall@k cho retrieval | Có golden set | Vừa |
| **4** | **LLM-as-judge** | Đầu ra là văn tự do, không cách nào khác | Đắt, **và bản thân judge cũng nhiễu** |

> **Bạn đang ở vị trí rất tốt cho tầng 1.** Kiến trúc rulebook của bạn khiến LLM trả về
> **`search_strategy` có schema**, không trả về văn xuôi — nên so sánh migration là so **field với
> field**, xác định và rẻ. Nói được điều này cho thấy bạn hiểu **thiết kế đầu ra có schema chính là
> thứ khiến hệ thống đo được**, chứ không chỉ là thứ giúp parse dễ.

**Nếu buộc dùng tầng 4:** phải cố định model judge và ghim version, chạy judge nhiều lần rồi lấy đa
số, và **hiệu chuẩn judge trên một tập người đã chấm** để biết judge lệch bao nhiêu. Judge không
hiệu chuẩn là đo bằng thước chưa biết dài bao nhiêu.

---

## 3. Cần bao nhiêu mẫu — công thức và ví dụ

Câu *"chạy bao nhiêu lần thì đủ"* có đáp án bằng công thức, không phải bằng cảm tính. Trả lời được
bằng số là tín hiệu rõ nhất rằng bạn thật sự làm được.

```text
        16 · p · (1 − p)
  n  ≈  ────────────────       ở power 80%, mức ý nghĩa 5%
              δ²

  p = tỉ lệ đúng hiện tại (baseline)
  δ = mức chênh lệch NHỎ NHẤT bạn muốn phát hiện được
  → rồi nhân thêm ~1,5× làm đệm cho phương sai của traffic thật
```

### Ví dụ tính bằng số — làm theo để nhớ

Hệ hiện tại đúng **85%**. Bạn muốn phát hiện được nếu nó tụt **3 điểm** (xuống 82%).

```text
p = 0,85     δ = 0,03

n ≈ 16 × 0,85 × 0,15 / 0,03²
  = 16 × 0,1275 / 0,0009
  = 2,04 / 0,0009
  ≈ 2.267 mẫu

× 1,5 đệm  ≈  3.400 mẫu
```

**Ba điều rút ra ngay từ công thức:**

1. **δ nằm ở mẫu số và bị bình phương.** Muốn phát hiện chênh lệch nhỏ đi **một nửa** thì cần
   **gấp bốn** số mẫu. Đây là lý do "chúng tôi test 50 case" gần như luôn không đủ để nói gì.
2. **p càng gần 0,5 thì càng cần nhiều mẫu.** Hệ đã rất tốt (p=0,95) thì kiểm chứng rẻ hơn.
3. Golden set 900 item của nghiên cứu ở [§1](#1-sai-lầm-gốc-accuracy-không-đổi-không-chứng-minh-gì)
   dùng **K=50 lần chạy mỗi item** — tức 45.000 lượt gọi. **Đó là mức nghiêm túc trông như thế nào.**

> **Câu ăn điểm:** *"Trước khi chạy tôi tính ngược từ mức chênh lệch cần phát hiện. Nếu tôi cần bắt
> được mức tụt 3 điểm trên baseline 85% thì cần cỡ 3.400 mẫu. Nếu ngân sách chỉ cho 500 mẫu thì tôi
> nói thẳng: với cỡ mẫu đó tôi chỉ phát hiện được mức tụt khoảng 8 điểm trở lên, và tôi sẽ **không**
> tuyên bố 'không có khác biệt' — tôi sẽ nói 'chưa đủ bằng chứng'. Hai câu đó rất khác nhau."*

**Phân biệt phải nói cho đúng:** *"không tìm thấy khác biệt"* ≠ *"không có khác biệt"*. Nhầm hai câu
này là lỗi thống kê phổ biến nhất, và người phỏng vấn giỏi sẽ nghe ra ngay.

---

## 4. Eval harness: đo cái gì, ở tầng nào

Migration LLM đo ở **bốn tầng**. Rất nhiều người chỉ đo tầng 3 rồi tưởng đã xong.

```text
┌── T1  BẤT BIẾN  (luôn luôn, mọi request, cả production) ──────────────────┐
│   JSON parse được · schema hợp lệ · mã nằm trong danh mục hợp lệ         │
│   không rò PII · độ dài trong giới hạn                                    │
│   → chạy được ngay trên traffic thật, không cần golden set                │
└──────────────────────────────────────────────────────────────────────────┘
┌── T2  CHẤT LƯỢNG THEO TÁC VỤ  (golden set, offline) ─────────────────────┐
│   accuracy theo từng field · P⁺ / P⁻ theo từng item · Recall@k           │
│   → đây là nơi áp toàn bộ §1 và §3                                        │
└──────────────────────────────────────────────────────────────────────────┘
┌── T3  VẬN HÀNH  (liên tục) ──────────────────────────────────────────────┐
│   TTFT p50/p95 · tokens/s prefill và decode TÁCH RIÊNG                    │
│   độ dài hàng đợi · GPU util · VRAM · tỉ lệ lỗi parse                     │
└──────────────────────────────────────────────────────────────────────────┘
┌── T4  NGHIỆP VỤ  (chậm nhất nhưng thật nhất) ────────────────────────────┐
│   tỉ lệ người dùng sửa lại kết quả · tỉ lệ escalate NEEDS_REVIEW          │
│   → tín hiệu chậm vài ngày, nhưng là thứ DUY NHẤT phản ánh giá trị thật  │
└──────────────────────────────────────────────────────────────────────────┘
```

**T1 là tầng bị bỏ qua nhiều nhất và rẻ nhất.** Nó không cần golden set, chạy được trên 100% traffic
production, và bắt được ngay lớp lỗi nguy hiểm nhất của self-host: model yếu phá schema. Bạn **đã có
sẵn** phần lớn tầng này — grounding gate kiểm mã hàng, `confidence_score`, `needs_human_review`.

**T4 là tầng thuyết phục nhất trong phòng phỏng vấn**, vì nó cho thấy bạn phân biệt được *chỉ số kỹ
thuật* với *giá trị nghiệp vụ*. Accuracy tăng mà người dùng vẫn sửa tay như cũ thì migration đó
không mang lại gì.

### Golden set — ba tính chất bắt buộc

| Tính chất | Vì sao | Hỏng thì sao |
|---|---|---|
| **Đại diện** cho phân phối thật | Golden toàn case dễ thì đo ra số đẹp vô nghĩa | Tự tin sai |
| **Ghim version, không đổi giữa hai lần đo** | Đổi golden giữa chừng là so hai thứ khác nhau | Kết luận vô giá trị |
| **Lưu toàn bộ log phản hồi thô** | Để chấm lại theo tiêu chí khác **mà không phải gọi lại model** | Không tái lập được |

> Tính chất thứ ba là khuyến nghị trực tiếp từ nghiên cứu ở [§1](#1-sai-lầm-gốc-accuracy-không-đổi-không-chứng-minh-gì):
> *"lưu trữ toàn bộ log phản hồi để có thể kiểm chứng và chấm lại theo cách khác mà không cần truy
> vấn lại các API thương mại vốn hay thay đổi."* API cloud có thể đổi model dưới chân bạn — nếu
> không lưu log thì baseline cũ **không dựng lại được nữa**.

---

## 5. Năm giai đoạn migrate không ảnh hưởng hệ đang chạy

Đây là câu trả lời cho *"migrate mà không impact tới hiện tại"*. Nguyên tắc xuyên suốt: **người dùng
không được thấy gì cho tới khi bạn có bằng chứng.**

```text
GĐ 0  SEAM + ĐO BASELINE          người dùng: 100% cloud
      ├─ tầng app chỉ nói một giao thức OpenAI-compatible
      ├─ dựng eval harness, chạy trên CLOUD trước để có baseline
      └─ LƯU TOÀN BỘ log phản hồi
      ✅ Qua khi: baseline có số, tái lập được hai lần liên tiếp

GĐ 1  SHADOW (dark launch)        người dùng: 100% cloud
      ├─ mỗi request gửi CẢ HAI bên; chỉ TRẢ VỀ kết quả cloud
      ├─ kết quả self-host ghi log, không ai thấy
      └─ so sánh trên traffic THẬT, không phải golden
      ⚠️ Bắt buộc: self-host phải chạy BẤT ĐỒNG BỘ, không được nằm trên
         đường phản hồi — nếu không, self-host chậm sẽ làm chậm khách
      ✅ Qua khi: P⁻ dưới ngưỡng, tỉ lệ lỗi schema chấp nhận được, đủ cỡ mẫu §3

GĐ 2  CANARY                      người dùng: 95% cloud / 5% self-host
      ├─ chia theo TENANT hoặc theo LOẠI REQUEST, không chia ngẫu nhiên
      │  (ngẫu nhiên → cùng một khách lúc được lúc không, khó debug)
      ├─ bắt đầu bằng loại request AN TOÀN NHẤT: schema chặt, hậu quả thấp
      └─ tự động rollback khi vượt ngưỡng
      ✅ Qua khi: 5% chạy ổn định đủ lâu, T4 không xấu đi

GĐ 3  TĂNG DẦN                    25% → 50% → 100%
      └─ mỗi nấc giữ đủ lâu để tín hiệu T4 (chậm) kịp hiện ra

GĐ 4  DỌN DẸP                     giữ đường lui thêm ≥1 chu kỳ
      └─ vẫn giữ được khả năng quay lại cloud bằng MỘT biến môi trường
```

**Bốn chi tiết quyết định thành bại, và là chỗ nên nói kỹ:**

| Chi tiết | Vì sao quan trọng |
|---|---|
| **Shadow phải bất đồng bộ** | Nếu gọi self-host đồng bộ để so, bạn vừa **thêm độ trễ cho khách** — đúng cái mình hứa không đụng tới. Fire-and-forget, so offline |
| **Canary chia theo tenant, không ngẫu nhiên** | Ngẫu nhiên làm cùng một khách lúc được kết quả A lúc B → không tái lập, không debug được, và khách cảm nhận là "hệ thống lúc tốt lúc tệ" |
| **Rollback tự động, không phải quyết định của người** | Người ngủ. Ngưỡng phải là code: `P⁻ > x` hoặc `lỗi schema > y` hoặc `p95 > z` → tự chuyển về cloud |
| **Bắt đầu từ loại request an toàn nhất** | Không phải 5% ngẫu nhiên, mà là 5% có schema chặt nhất và hậu quả thấp nhất |

> **Câu trả lời gọn cho "làm sao không impact":** *"Người dùng không thấy gì cho tới giai đoạn 2, và
> ở giai đoạn 1 tôi đã có bằng chứng trên traffic thật rồi. Shadow chạy bất đồng bộ nên không thêm
> một mili-giây nào vào đường phản hồi. Và ở mọi giai đoạn, đường lui về cloud chỉ là một biến môi
> trường — rollback là tự động theo ngưỡng, không chờ ai quyết định."*

---

## 6. CI gate cho hệ không tất định

Câu hỏi *"test tự động thế nào"* — và đây là chỗ nhiều người lúng túng vì quen với test tất định.

### Ba loại test, ba tốc độ

| Loại | Chạy khi | Thời gian | Gate |
|---|---|:---:|---|
| **Bất biến (T1)** | Mỗi commit | giây | **Cứng** — vi phạm là đỏ ngay. `schema hợp lệ 100%` |
| **Golden nhỏ** (~50-100 item, K=5) | Mỗi PR | phút | **Mềm** — cảnh báo, không chặn. Cỡ mẫu chưa đủ để kết luận |
| **Golden đầy đủ** (§3, K lớn) | Đêm / trước release | giờ | **Cứng** — `P⁻ ≤ ngưỡng` |

**Điểm mấu chốt phải hiểu:** golden nhỏ trong PR **không đủ cỡ mẫu để kết luận** ([§3](#3-cần-bao-nhiêu-mẫu--công-thức-và-ví-dụ)),
nên nó **không được** là gate cứng. Đặt gate cứng lên một phép đo nhiễu thì CI sẽ đỏ ngẫu nhiên, và
sau ba tuần cả team sẽ bỏ qua nó — tệ hơn là không có gate.

### Viết gate thế nào cho đúng

```python
# SAI — đỏ ngẫu nhiên, rồi cả team học cách bỏ qua
assert output == "expected string"
assert accuracy >= 0.85              # một lần chạy, cỡ mẫu nhỏ

# ĐÚNG — gate trên thứ tất định
assert all(is_valid_schema(o) for o in outputs)          # bất biến: cứng được
assert all(code in KNOWN_CODES for code in extracted)    # grounding: cứng được

# ĐÚNG — gate trên thứ không tất định, phát biểu theo thống kê
result = compare_item_level(baseline_runs, candidate_runs, k=20)
assert result.reliable_regression_share <= 0.02, (
    f"{result.reliable_regression_share:.1%} số item tụt hạng đáng tin "
    f"(ngưỡng 2%). Danh sách: {result.regressed_ids[:10]}"
)
assert result.inconclusive_share <= 0.30, "cỡ mẫu chưa đủ — tăng K rồi chạy lại"
```

**Dòng cuối là dòng nhiều người quên.** Nếu tỉ lệ "chưa kết luận được" quá cao thì kết quả không có
giá trị — và im lặng bỏ qua nó là tự lừa mình. Gate phải **fail vì thiếu bằng chứng**, không chỉ
fail vì có bằng chứng xấu.

### Chống flaky ngay từ thiết kế

| Nguồn flaky | Cách chặn |
|---|---|
| Sampling ngẫu nhiên | `temperature=0` cho eval, **và** vẫn chạy K lần vì [§2 tầng 2](#2-vì-sao-không-thể-diff-đầu-ra-kể-cả-ở-temperature-0) |
| Model đổi dưới chân | **Ghim version**. Với cloud: ghim snapshot id. Với self-host: ghim digest ảnh + hash trọng số |
| Golden set trôi | Version hoá golden, review thay đổi như review code |
| Prompt đổi mà không ai biết | 1 file 1 prompt, có version trong front-matter — **bạn đã làm** |
| Judge trôi | Ghim model judge, chạy nhiều lần lấy đa số, hiệu chuẩn định kỳ |

---

## 7. Capacity: tính từ nguyên lý, KV cache mới là trần thật

Câu *"bao nhiêu user cùng lúc thì chịu được"* phải trả lời bằng phép tính, không phải bằng "chúng
tôi sẽ benchmark".

### VRAM đi đâu

```text
VRAM  =  TRỌNG SỐ (cố định)  +  KV CACHE (tỉ lệ với số request đang chạy)  +  overhead

Ví dụ có thật (H100 80GB, model 70B FP8):
    trọng số 70B FP8         ≈ 70 GB
    overhead + activation     ≈ vài GB
    ────────────────────────────────
    còn lại cho KV cache      ≈ 10 GB   ◄── ĐÂY là thứ giới hạn số user đồng thời
```

### KV cache quy ra bao nhiêu token đang bay

Cũng ví dụ trên, với ~10 GB KV pool:

| KV dtype | Số block | Kích thước block | Token đồng thời |
|---|---|---|---|
| **BF16** | ~2.000 | 5 MB | **~32.000 token** |
| **FP8** | ~4.000 | 2,5 MB | **~64.000 token** |

> **Kết luận rút ra thẳng từ bảng:** **giảm nửa dtype của KV cache thì gấp đôi số user đồng thời** —
> mà **không đụng gì tới trọng số model**. Đây là nút vặn hiệu quả nhất mà ít người biết, và nói ra
> được là tín hiệu rất rõ rằng bạn đã đọc tới tầng serving.

### Từ token quy ra số user

```text
Số request đồng thời  ≈  KV_pool_tokens / (token trung bình mỗi request đang giữ)

Ví dụ:  KV pool = 64.000 token (FP8)
        mỗi request giữ trung bình 2.000 token (prompt + phần đã sinh)
        → ~32 request ĐANG CHẠY cùng lúc trên MỘT GPU
```

Rồi dùng **định luật Little** để ra throughput:

```text
        L = λ × W          L = số request trong hệ, λ = tốc độ đến, W = thời gian trong hệ

  L = 32 request đồng thời
  W = 4 giây trung bình mỗi request
  → λ = L / W = 32 / 4 = 8 request/giây  =  ~28.800 request/giờ  trên MỘT GPU
```

**Đây là con số bạn mang vào phòng phỏng vấn.** Không phải "tuỳ", mà là một phép tính có giả định
nói rõ ra được.

### Prefill và decode là hai nút cổ chai khác nhau

Đây là chỗ phân biệt người đã vận hành với người đã đọc:

| | **Prefill** (xử lý prompt) | **Decode** (sinh từng token) |
|---|---|---|
| Nghẽn ở | **Compute (FLOPs)** | **Băng thông bộ nhớ** |
| Song song hoá | Cao — cả prompt cùng lúc | Thấp — tuần tự từng token |
| Ảnh hưởng tới | **TTFT** (time to first token) | **TPOT** (thời gian mỗi token sau đó) |
| Prompt dài làm gì | TTFT tăng mạnh | Ít ảnh hưởng |
| Cách chữa | **Chunked prefill**, prefix caching | Batching lớn hơn, KV cache nhỏ hơn |

→ Vì vậy **đo `tokens/s` gộp là vô nghĩa**. Phải tách. Một hệ TTFT tệ và một hệ TPOT tệ cần hai cách
chữa hoàn toàn khác nhau.

### Quy trình chẩn đoán — học thuộc bảng này

Chạy `benchmark_serving.py` của vLLM ở mức đồng thời **10, 50, 100**, ghi tokens/s, TTFT p50/p95, và
GPU util (`nvidia-smi dmon`). Rồi đọc:

| Triệu chứng | Bạn đang bị gì | Vặn nút nào |
|---|---|---|
| GPU util **≥ 85%** | **GPU-bound** — hết sức tính | Thêm GPU, hoặc model nhỏ hơn / lượng tử hoá sâu hơn |
| TTFT cao ở mức đồng thời **vừa phải** | **KV-cache-bound** | Giảm `max-model-len`, đổi KV sang FP8, bật prefix caching |
| GPU util **< 60%** ở mức đồng thời **cao** | **Scheduler-bound** — GPU rảnh mà request vẫn chờ | Tăng `max-num-seqs`, tăng `max-num-batched-tokens` (8192–16384) |

> **Bảng này là câu trả lời hoàn chỉnh cho "hệ chậm thì anh làm gì".** Ba triệu chứng, ba nguyên
> nhân, ba nút vặn khác nhau. Trả lời "tôi sẽ thêm GPU" cho cả ba trường hợp là sai hai phần ba.

---

## 8. Không mất request khi tải cao

Đây là phần bạn nói bị hỏi: *"nhiều user cùng call thì làm sao đảm bảo không mất"*.

### Phân biệt hai khái niệm bị nhầm nhiều nhất

```text
ADMISSION  (nhận vào)   — quyết định khi nào request được VÀO tập đang chạy
SCHEDULING (xếp lịch)   — quyết định request đã vào thì chạy Ở ĐÂU, thứ tự nào

Nhầm hai cái này là nguồn gốc của mọi hệ "không bao giờ từ chối ai" rồi sập.
```

### Vì sao "không từ chối ai" là sai

Có đo thật, và con số rất rõ:

| | **Có admission control** | **Không có** |
|---|---|---|
| Request đang chạy | Đúng bằng công suất | Đúng bằng công suất |
| **Hàng đợi chờ** | **Rỗng** | **Phình tới 34 request** |
| **Độ trễ** | Ổn định | **Tăng liên tục, không có trần** |
| Request vượt công suất | Nhận **429 + Retry-After** ngay | Chờ mãi rồi timeout — **tệ hơn hẳn** |

**Nghịch lý phải hiểu:** hệ "không bao giờ từ chối" thực ra **làm mất nhiều request hơn** hệ có từ
chối. Vì client timeout rồi retry, tạo thêm tải, đẩy độ trễ lên nữa — vòng xoáy tử thần. Còn 429 kèm
`Retry-After` là **thông tin trung thực** để client lùi lại đúng cách.

Có một lý do kỹ thuật nữa đặc thù cho LLM: khi nhiều request cùng chạy, **bộ nhớ của chúng cùng
phình một lúc** tạo ra đỉnh tập trung → vLLM buộc phải **preempt** và tính lại từ đầu. Kiểm soát tốc
độ nhận vào làm bộ nhớ biến thiên mượt hơn, tức **tránh được cả việc mất công tính lại**.

### "Không mất" nghĩa là gì cho đúng

```text
KHÔNG mất  ≠  không bao giờ từ chối

KHÔNG mất  =  đã NHẬN thì PHẢI HOÀN THÀNH
              đã từ chối thì client BIẾT mình bị từ chối và biết khi nào thử lại
```

Nghĩa là ranh giới nằm ở chỗ bạn **trả lời gì**:

| Trả về | Cam kết | Nghĩa vụ của bạn |
|---|---|---|
| **200** đồng bộ | Xong rồi | Không có |
| **202 Accepted + job id** | **Tôi sẽ làm xong** | **Phải bền hoá job trước khi trả 202.** Sập rồi bật lại vẫn phải chạy tiếp |
| **429 + Retry-After** | Tôi không nhận | Không có — và đây là câu trả lời **trung thực**, không phải thất bại |

**Sai lầm chết người:** trả `202` rồi giữ job trong hàng đợi **trong bộ nhớ**. Process restart là mất
sạch, mà client đã được hứa. Đã hứa thì phải ghi xuống đĩa/DB/queue bền **trước khi** hứa.

### Kiến trúc đầy đủ cho tải cao

```text
        ┌──────────────────────────────────────────────────────────┐
Client →│ 1. QUOTA THEO TENANT   token bucket → 429 nếu vượt      │
        │    (chặn ở cửa, trước khi tốn bất kỳ tài nguyên nào)     │
        └────────────────────────┬─────────────────────────────────┘
                                 ▼
        ┌──────────────────────────────────────────────────────────┐
        │ 2. PHÂN LOẠI  ngắn/đồng bộ  vs  dài/bất đồng bộ         │
        │    ngắn → chờ luôn (200)                                 │
        │    dài  → GHI BỀN job + trả 202 + job id                 │
        └────────────────────────┬─────────────────────────────────┘
                                 ▼
        ┌──────────────────────────────────────────────────────────┐
        │ 3. ADMISSION  tập đang chạy có TRẦN CỨNG                 │
        │    đầy → hàng đợi CÓ GIỚI HẠN → đầy nữa → 429            │
        │    hàng đợi có trọng số theo tenant (chống noisy neighbor)│
        └────────────────────────┬─────────────────────────────────┘
                                 ▼
        ┌──────────────────────────────────────────────────────────┐
        │ 4. vLLM  continuous batching + prefix caching            │
        └────────────────────────┬─────────────────────────────────┘
                                 ▼
        ┌──────────────────────────────────────────────────────────┐
        │ 5. KẾT QUẢ ghi vào store; client poll hoặc nhận webhook  │
        │    idempotency key → gọi lại không sinh job trùng        │
        └──────────────────────────────────────────────────────────┘
        ┌──────────────────────────────────────────────────────────┐
        │ 6. FALLBACK  circuit breaker → quay về CLOUD khi tự host │
        │    quá tải hoặc chết. Đây là "không mất" mạnh nhất       │
        └──────────────────────────────────────────────────────────┘
```

**Bước 6 là điểm mạnh riêng của bài toán migration này** — và là chỗ nên nhấn: vì bạn **vẫn còn
đường cloud**, quá tải không phải là mất request mà chỉ là **đắt hơn tạm thời**. Rất ít hệ thống có
sẵn một fallback tốt như vậy, và nó là hệ quả trực tiếp của việc giữ đúng seam ở
[§5 GĐ 0](#5-năm-giai-đoạn-migrate-không-ảnh-hưởng-hệ-đang-chạy).

**Nối với phần đã có:** hàng đợi có trọng số theo tenant ở bước 3 chính là bài
[02 — Distributed test execution](katalon-system-design/02-distributed-test-execution.md#biến-thể-hay-bị-hỏi-một-tenant-gửi-10000-test-cùng-lúc-mà-không-ảnh-hưởng-tenant-khác);
idempotency key ở bước 5 chính là lập luận ở
[06 §6 bước 1](katalon-system-design/06-race-condition-balance-ledger.md#bước-1--idempotency-trước-tiên-làm-trước-cả-việc-sửa-race).
**Bạn đã có sẵn hai mảnh này** — chỉ là chưa ghép vào ngữ cảnh LLM.

---

## 9. Scale ngang: nhiều replica, routing, autoscale GPU

### GPU không scale như container — và đây là điều phải nói

```text
Container stateless:  scale up trong VÀI GIÂY, scale-to-zero thoải mái
GPU chạy LLM:         nạp trọng số 70B mất VÀI PHÚT
                      → autoscale phản ứng LUÔN LUÔN quá muộn
```

**Bốn hệ quả trực tiếp:**

1. **Luôn giữ dư công suất**, đừng chạy sát trần. Tối ưu tới 95% util là tự sát khi có burst.
2. **Scale theo độ dài hàng đợi và TTFT**, không theo GPU util. Util là chỉ báo trễ.
3. **Scale lên sớm, scale xuống muộn** — bất đối xứng có chủ đích.
4. **Hàng đợi bền là tấm đệm hấp thụ** khoảng thời gian chờ replica mới lên. Đây là lý do bước 2 ở
   [§8](#8-không-mất-request-khi-tải-cao) không phải tuỳ chọn.

### Routing giữa các replica: đừng dùng round-robin

| Cách | Vấn đề |
|---|---|
| **Round-robin** | Bỏ phí prefix cache — request có cùng system prompt đi vào replica khác nhau, mỗi nơi tính lại prefill từ đầu |
| **Least-connections** | Khá hơn, nhưng vẫn không tận dụng cache |
| **Prefix-aware routing** ✅ | Đưa request có **cùng tiền tố** về **cùng replica** → prefill gần như miễn phí nhờ prefix caching |

> Với hệ có system prompt dài hoặc few-shot cố định — tức gần như mọi hệ production —
> **prefix-aware routing tiết kiệm nhiều hơn mọi tối ưu prompt cộng lại**, vì nó cắt hẳn pha tốn
> nhất là prefill.

### Ba nút vặn trước khi thêm GPU

Theo đúng thứ tự tỉ lệ hoàn vốn:

1. **Prefix caching** — miễn phí nếu traffic có tiền tố chung
2. **KV cache FP8** — gấp đôi số user đồng thời, [§7](#7-capacity-tính-từ-nguyên-lý-kv-cache-mới-là-trần-thật)
3. **Chunked prefill** — prompt dài không còn chặn các request đang decode

Ba nút này **không đụng tới chất lượng**. Sau đó mới tới lượng tử hoá sâu hơn (có đụng chất lượng,
phải đo lại theo [§1](#1-sai-lầm-gốc-accuracy-không-đổi-không-chứng-minh-gì)), rồi mới tới thêm GPU.

---

## 10. Kịch bản trả lời + 12 follow-up

### 10.1 Trình bày 5 phút — cấu trúc

```text
0:00-0:40  ĐẶT LẠI ĐỀ BÀI
  "Bài này có ba ràng buộc, và ràng buộc khó nhất không phải hạ tầng.
   Một, không được ảnh hưởng hệ đang chạy. Hai, phải CHỨNG MINH kết quả
   được bảo toàn — mà đầu ra LLM không tất định nên không diff được.
   Ba, chịu tải đồng thời mà không mất request.
   Tôi sẽ đi theo thứ tự đó, vì cái thứ hai chi phối cả cái thứ nhất."

0:40-1:30  SEAM  (§5 GĐ 0)
  "Việc đầu tiên không phải chọn model, mà là tầng app chỉ nói MỘT giao thức.
   Không có seam thì không có đường lui, mà không có đường lui thì không ai dám chuyển."

1:30-2:50  BẢO TOÀN KẾT QUẢ  ◄── DÀNH NHIỀU THỜI GIAN NHẤT CHO PHẦN NÀY
  "Accuracy tổng KHÔNG chứng minh được gì — nó che mất thay đổi ở tầng item.
   Cái tôi gate là tỉ lệ item TỤT HẠNG đáng tin. Chạy mỗi item K lần,
   Fisher exact có hiệu chỉnh FDR, cộng ngưỡng độ lớn tối thiểu,
   rồi hiệu chỉnh bằng permutation null.
   Cỡ mẫu tính ngược từ mức chênh lệch cần phát hiện: n ≈ 16p(1−p)/δ²."

2:50-3:40  TRIỂN KHAI DẦN  (§5)
  "Shadow bất đồng bộ → canary theo tenant → tăng dần.
   Rollback tự động theo ngưỡng, không chờ người quyết."

3:40-4:40  CAPACITY VÀ KHÔNG MẤT REQUEST  (§7, §8)
  "VRAM = trọng số + KV cache; KV cache mới là trần số user đồng thời.
   KV FP8 gấp đôi công suất mà không đụng model.
   Little's Law ra throughput. Admission control có trần cứng —
   nghịch lý là hệ 'không bao giờ từ chối' lại mất nhiều request hơn."

4:40-5:00  ĐÁNH ĐỔI VÀ RANH GIỚI
  "Cái tôi đã làm, cái tôi chưa làm, và con số nào tôi chưa có."
```

**Phân bổ thời gian là thông điệp.** Dành nhiều nhất cho phần *bảo toàn kết quả* cho thấy bạn hiểu
đâu mới là phần khó — chứ không phải hạ tầng.

### 10.2 Follow-up

<details>
<summary><b>Nhóm A — bảo toàn kết quả (5 câu)</b></summary>

**A1. "Làm sao anh biết chuyển xong kết quả không đổi?"**
> Tôi không dùng accuracy tổng làm bằng chứng — nó che mất thay đổi ở tầng từng item. Một mức tăng
> ròng 2 điểm có thể là 100 câu tốt lên và 80 câu xấu đi, và 80 khách hàng đó nhận kết quả tệ hơn
> trong khi bảng số nói "không đổi". Cái tôi gate là **tỉ lệ item tụt hạng đáng tin**: chạy mỗi item
> K lần để ước lượng xác suất đúng, so bằng Fisher exact có hiệu chỉnh FDR vì đang test hàng trăm
> item cùng lúc, cộng một ngưỡng độ lớn tối thiểu để lọc thay đổi vụn vặt.

**A2. "Sao không đặt temperature 0 rồi so chuỗi?"**
> Vì `temperature=0` chỉ tắt được tầng bất định thứ nhất. Còn tầng thứ hai không tắt được khi còn
> continuous batching: batch khác nhau làm thứ tự cộng dấu phẩy động khác nhau, mà cộng float không
> có tính kết hợp — chênh lệch ở chữ số cuối đôi khi đủ để lật argmax. Nên cùng prompt, cùng
> temperature 0, hai lần chạy ở mức tải khác nhau vẫn có thể ra hai chuỗi khác nhau. Vì vậy tôi so
> **phân phối qua K lần chạy**, không so một kết quả.

**A3. "Cần bao nhiêu mẫu?"**
> Tính ngược từ mức chênh lệch cần phát hiện: `n ≈ 16·p(1−p)/δ²` ở power 80%, thêm khoảng 1,5× đệm
> cho phương sai traffic thật. Baseline 85%, muốn bắt được mức tụt 3 điểm thì cần cỡ 3.400 mẫu.
> Điều quan trọng là δ bị bình phương ở mẫu số — muốn phát hiện chênh lệch nhỏ đi một nửa thì cần
> gấp bốn số mẫu. Đó là lý do "chúng tôi test 50 case" gần như luôn không kết luận được gì.

**A4. "Nếu không đủ ngân sách chạy nhiều mẫu?"**
> Tôi sẽ nói rõ giới hạn thay vì giả vờ. Với 500 mẫu tôi chỉ phát hiện được mức tụt khoảng 8 điểm
> trở lên, nên tôi sẽ báo cáo là **"chưa đủ bằng chứng"**, không phải "không có khác biệt" — hai câu
> đó rất khác nhau. Và tôi sẽ dồn ngân sách vào tập request có hậu quả cao nhất thay vì rải đều, rồi
> bù bằng theo dõi tầng bất biến trên 100% traffic production.

**A5. "Làm sao biết cái anh đo là tín hiệu chứ không phải nhiễu?"**
> Permutation null: trộn ngẫu nhiên nhãn "cloud" và "self-host" rồi chạy lại **toàn bộ** quy trình
> phân loại. Nếu quy trình vẫn tìm ra "thay đổi đáng tin" trên dữ liệu đã trộn thì nó đang tạo kết
> quả giả và tôi phải siết ngưỡng lại. Đây là bước tôi thấy hay bị bỏ nhất, mà nó chính là thứ trả
> lời được câu hỏi này.
</details>

<details>
<summary><b>Nhóm B — triển khai và test (4 câu)</b></summary>

**B1. "Shadow mode làm sao không làm chậm khách?"**
> Gọi self-host **bất đồng bộ, fire-and-forget**, không nằm trên đường phản hồi. Nếu gọi đồng bộ để
> so thì tôi vừa thêm độ trễ cho khách — đúng cái mình hứa không đụng tới. So sánh làm offline từ
> log. Và shadow phải có quota riêng để nó không tranh tài nguyên với đường chính.

**B2. "Canary chia thế nào?"**
> Theo **tenant hoặc theo loại request**, không chia ngẫu nhiên. Ngẫu nhiên làm cùng một khách lúc
> được kết quả A lúc B — không tái lập, không debug được, và khách cảm nhận là hệ thống lúc tốt lúc
> tệ. Tôi bắt đầu bằng loại request có schema chặt nhất và hậu quả thấp nhất, không phải 5% ngẫu
> nhiên.

**B3. "CI gate đặt ở đâu khi output không tất định?"**
> Ba tầng, ba tốc độ. Tầng bất biến — schema hợp lệ, mã nằm trong danh mục — là **tất định** nên gate
> cứng được, chạy mỗi commit. Golden nhỏ trong PR là gate **mềm** vì cỡ mẫu chưa đủ để kết luận —
> đặt gate cứng lên phép đo nhiễu thì CI đỏ ngẫu nhiên và sau ba tuần cả team bỏ qua nó, tệ hơn là
> không có. Golden đầy đủ chạy đêm mới là gate cứng, trên `P⁻`. Và tôi gate cả **tỉ lệ chưa kết luận
> được** — fail vì thiếu bằng chứng cũng là fail.

**B4. "Rollback thế nào?"**
> Một biến môi trường, vì seam đã đúng. Nhưng quan trọng hơn: rollback là **tự động theo ngưỡng**,
> không phải quyết định của người — `P⁻` vượt ngưỡng, hoặc lỗi schema vượt ngưỡng, hoặc p95 vượt
> ngưỡng thì tự chuyển về cloud. Người ngủ, ngưỡng thì không.
</details>

<details>
<summary><b>Nhóm C — capacity và không mất request (3 câu)</b></summary>

**C1. "Một GPU phục vụ được bao nhiêu user cùng lúc?"**
> Tính được, không phải "tuỳ". VRAM = trọng số + KV cache; trọng số cố định nên **KV cache mới là
> trần**. Ví dụ H100 80GB với model 70B FP8 còn khoảng 10GB cho KV pool — với KV BF16 là khoảng
> 32.000 token đang bay, với FP8 là 64.000. Chia cho số token trung bình mỗi request đang giữ ra số
> request đồng thời; ví dụ 2.000 token/request thì được khoảng 32 request. Rồi Little's Law:
> L = λ×W, 32 request với 4 giây mỗi request ra 8 request/giây. Và điểm đáng nói: **giảm nửa dtype
> của KV cache thì gấp đôi số user, không đụng gì tới model**.

**C2. "Nhiều user gọi cùng lúc, làm sao không mất request?"**
> Trước hết phải định nghĩa "không mất" cho đúng: nó **không** có nghĩa là không bao giờ từ chối. Nó
> có nghĩa là đã nhận thì phải hoàn thành, và đã từ chối thì client biết mình bị từ chối và biết khi
> nào thử lại. Nghịch lý là hệ "không bao giờ từ chối" lại **mất nhiều request hơn** — hàng đợi
> phình, độ trễ tăng không có trần, client timeout rồi retry tạo thêm tải. Có đo cho thấy admission
> control giữ hàng đợi rỗng, còn không có nó thì hàng đợi phình tới 34 request và độ trễ tăng liên
> tục. Nên: quota theo tenant ở cửa, tập đang chạy có trần cứng, hàng đợi có giới hạn, vượt nữa thì
> 429 kèm `Retry-After`. Việc dài thì ghi bền job rồi trả 202 — và phải ghi **trước khi** trả 202,
> vì trả 202 là đã hứa.

**C3. "Hệ chậm thì anh làm gì đầu tiên?"**
> Chẩn đoán trước khi vặn. Chạy benchmark ở mức đồng thời 10, 50, 100 rồi đọc ba triệu chứng: GPU
> util trên 85% là **GPU-bound**, phải thêm sức tính; TTFT cao ở mức đồng thời vừa phải là
> **KV-cache-bound**, giảm `max-model-len` hoặc chuyển KV sang FP8; GPU util dưới 60% mà request vẫn
> chờ là **scheduler-bound**, tăng `max-num-seqs` và `max-num-batched-tokens`. Ba nguyên nhân, ba nút
> vặn khác nhau — trả lời "thêm GPU" cho cả ba là sai hai phần ba. Và tôi đo `tokens/s` **tách riêng
> prefill và decode**, vì prefill nghẽn ở compute còn decode nghẽn ở băng thông bộ nhớ.
</details>

---

## 11. Lộ trình học 4 tuần — có số đo

Đây là phần biến tài liệu thành kỹ năng. **Mỗi tuần phải kết thúc bằng một con số của chính bạn**,
không phải một chương đã đọc.

### Tuần 1 — Đo được trước đã

| Việc | Xong khi có |
|---|---|
| Đóng băng một golden set từ dữ liệu thật, **version hoá**, ~200 item | File golden có hash, commit vào git |
| Viết runner chạy golden **K lần mỗi item**, lưu **toàn bộ** log thô | Thư mục log tái chạy lại được |
| Đo baseline trên hệ hiện tại, chạy **hai lần** | Hai lần chênh nhau bao nhiêu — **đó là mức nhiễu nền của bạn** |
| Tính cỡ mẫu cần cho δ bạn quan tâm | Một con số cụ thể, ghi vào README |

> Dòng thứ ba là dòng quan trọng nhất tuần này. **Chạy baseline hai lần trên cùng một hệ** cho bạn
> biết nhiễu nền — và mọi khác biệt nhỏ hơn nhiễu nền đều vô nghĩa. Rất ít người làm bước này.

### Tuần 2 — So sánh cho đúng

| Việc | Xong khi có |
|---|---|
| Viết hàm so **theo từng item**, ra P⁺ / P⁻ / tương đương / chưa kết luận | Bảng 4 con số |
| Cài Fisher exact + hiệu chỉnh Benjamini-Hochberg (`scipy.stats` có sẵn) | Chạy được trên golden thật |
| Chạy **permutation null**: trộn nhãn, chạy lại, đếm số "thay đổi đáng tin" giả | Con số đó **phải gần 0** |
| Dựng bảng so cloud vs self-host trên chính golden của bạn | **P⁻ thật của hệ bạn** |

### Tuần 3 — Capacity bằng tay

| Việc | Xong khi có |
|---|---|
| Tính VRAM: trọng số + KV pool trên đúng GPU bạn đang dùng | Số token đồng thời tối đa |
| Chạy `benchmark_serving.py` ở mức đồng thời 10 / 50 / 100 | Bảng tokens/s, TTFT p50/p95, GPU util |
| Xác định bạn đang **GPU-bound / KV-bound / scheduler-bound** | Một kết luận, có số chứng minh |
| Đổi KV sang FP8, đo lại | **Số user đồng thời trước và sau** |
| Áp Little's Law ra throughput lý thuyết, so với đo thật | Chênh bao nhiêu, và vì sao |

### Tuần 4 — Không mất request

| Việc | Xong khi có |
|---|---|
| Thêm admission control có trần cứng + hàng đợi giới hạn | Vượt trần trả 429 + `Retry-After` |
| Load test vượt công suất **2×**, đo hàng đợi và p99 | Hai đường cong: có và không có admission control |
| Thêm ghi bền job + trả 202 cho việc dài, có idempotency key | **Giết process giữa chừng, bật lại, job vẫn xong** |
| Thêm circuit breaker fallback về cloud | Tắt self-host, hệ vẫn phục vụ |

**Phép thử cuối cùng — đây là bài kiểm tra thật:**

```text
Chạy load test 2× công suất trong 10 phút, trong lúc đó:
   • giết một replica self-host
   • bật lại
   • kill process API giữa chừng

Đạt khi:  MỌI request đã nhận 200 hoặc 202 đều hoàn thành
          MỌI request bị từ chối đều nhận 429 kèm Retry-After
          KHÔNG có request nào biến mất không dấu vết
          p99 có trần, không tăng liên tục
```

Làm xong bài này thì câu hỏi *"làm sao đảm bảo không mất request"* bạn trả lời bằng **kết quả đo của
chính mình**, không bằng lý thuyết. Và đó là khác biệt giữa lần trượt vừa rồi với lần sau.

---

## 12. Ranh giới trung thực

| Điều | Trạng thái | Nói gì |
|---|---|---|
| Kiến thức §1–§9 | Chuẩn ngành, có nguồn 2026 | ✅ Nói tự tin **về nguyên lý** |
| **Số cụ thể** (10GB KV pool, 32k/64k token, hàng đợi phình tới 34, n≈16p(1−p)/δ²) | 🟠 **Nguồn ngoài, bạn chưa đo** | 🟠 Nói *"theo tài liệu vLLM / nghiên cứu 2026"*. **Đừng nói "tôi đo được"** cho tới hết tuần 3 |
| Phát hiện "accuracy tổng che regression" | 🟢 Nghiên cứu 2026, có số cụ thể | ✅ Trích được, nói rõ là nghiên cứu |
| Bất định do batch ở temperature 0 | 🟢 Hệ quả của số học dấu phẩy động | ✅ Giải thích được bằng nguyên lý |
| Eval harness item-level | 🔴 **Bạn chưa dựng** | 🔴 Nói *"đây là cách tôi sẽ làm"*, không nói đã làm. Tuần 1–2 để đổi được câu này |
| Shadow / canary | 🔴 **Chưa triển khai** | 🔴 Là kế hoạch |
| Số capacity của hệ bạn | 🔴 **Chưa đo** | 🔴 Tuần 3 |
| Admission control | 🔴 **Chưa có** | 🔴 Tuần 4 |
| Seam OpenAI-compatible | ✅ **Đã có, đang chạy** | ✅ Dẫn chứng `.env` được — [katalon-selfhost-llm.md §1](katalon-selfhost-llm.md) |
| Đo Qwen vs Claude | ✅ Đã đo — nhưng **theo accuracy tổng, chưa theo item-level** | 🟡 Nói thẳng: *"tôi đã đo, nhưng ở mức tổng — và tôi biết mức đó chưa đủ, đây là cách tôi sẽ làm chặt hơn"* |

> **Câu trung thực mạnh nhất bạn có thể nói** — và nó mạnh hơn hẳn việc giả vờ đã làm:
>
> *"Tôi đã chuyển hệ của mình sang self-host và đã đo chất lượng đối chiếu model cloud. Nhưng tôi đo
> ở mức **accuracy tổng**, và sau này tôi mới hiểu mức đó chưa đủ: nó che mất thay đổi ở tầng từng
> item. Nếu làm lại, tôi sẽ gate trên tỉ lệ item tụt hạng đáng tin, chạy mỗi item nhiều lần, hiệu
> chỉnh đa kiểm định, và hiệu chuẩn bằng permutation null. Tôi đã biết mình thiếu gì và đang bịt
> đúng chỗ đó."*
>
> Người phỏng vấn ở level này **không tìm người biết hết**. Họ tìm người **biết mình chưa biết gì và
> biết cách bịt** — vì đó mới là thứ dự đoán được bạn làm việc thế nào khi gặp bài toán mới.

---

## Liên quan

| Tài liệu | Liên quan chỗ nào |
|---|---|
| [katalon-selfhost-llm.md](katalon-selfhost-llm.md) | Câu chuyện và kinh tế học self-host — file này là phần **kỹ năng kỹ thuật** đi kèm |
| [AI-STACK-INTERVIEW-ANSWERS.md §7.3](AI-STACK-INTERVIEW-ANSWERS.md) | Số đo Qwen vs Claude — baseline cần được đo lại theo [§1](#1-sai-lầm-gốc-accuracy-không-đổi-không-chứng-minh-gì) |
| [katalon-system-design/02](katalon-system-design/02-distributed-test-execution.md) | Hàng đợi có trọng số, fairness — dùng lại ở [§8](#8-không-mất-request-khi-tải-cao) |
| [katalon-system-design/04](katalon-system-design/04-event-counting-10k.md) | Backpressure, 202 Accepted, hàng đợi có giới hạn |
| [katalon-system-design/06](katalon-system-design/06-race-condition-balance-ledger.md) | Idempotency là tiền đề của retry |
| [katalon-self-questions/01](katalon-self-questions/01-deadlock.md) | Mẫu tài liệu: chạy thật rồi mới ghi số |
