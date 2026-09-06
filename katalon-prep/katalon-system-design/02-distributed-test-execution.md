# 02 — Design Distributed Test Execution Platform

> Họ bài: **điều phối tác vụ phân tán & chia tài nguyên hữu hạn** (job scheduling / fair queueing).
> Chữ ký nhận dạng: *nhiều bên tranh nhau một pool tài nguyên hữu hạn*.
> Tách ra từ `katalon-senior-lead-phong-van.md` §5.2, có bổ sung phần multi-tenant fairness.
> Khung 45 phút: [README](README.md#6-khung-45-phút).

**Đề:** Chạy hàng triệu test case song song cho nhiều tenant, trên nhiều browser/OS.

## Điểm cần nêu (execution platform)

- **Scheduling:** không chia test đều theo số lượng, mà **bin-packing theo thời lượng lịch sử** (LPT — longest processing time first) để cân shard. Đây chính là họ bài toán #253 Meeting Rooms II / interval scheduling.
- **Fairness:** **weighted fair queueing per tenant** — tenant free không được làm nghẽn tenant enterprise. Quota + priority class.
- **Worker:** K8s **Job** per test-shard. **Warm pool** để tránh cold start (pull image browser rất nặng) — đánh đổi: idle cost vs latency.
- **Isolation:** mỗi run 1 container riêng (browser state, cookie, download). Network policy chặn cross-tenant.
- **Artifact:** screenshot/video/trace → S3 qua **presigned URL upload trực tiếp từ worker**, không qua API server (tránh nghẽn băng thông).
- **Log streaming:** worker → Kafka → WebSocket/SSE tới UI; không poll DB.
- **Retry & flaky:** phân biệt **fail thật** vs **flaky** — retry tự động N lần, nếu pass sau retry thì gắn nhãn flaky và tính **flakiness score** theo thời gian, không âm thầm cho pass.
- **Timeout budget:** timeout theo tầng (test < shard < run), có kill switch, cleanup orphan pod.
- **Cost:** spot instance + checkpoint để chịu được bị thu hồi; scale-to-zero ngoài giờ; **KEDA** scale theo độ dài queue Kafka thay vì CPU.

## Trade-off (execution platform)

| | Được | Mất |
|---|---|---|
| Warm pool | p50 latency thấp | Trả tiền cho node idle |
| Spot instance | Rẻ 60–70% | Bị thu hồi giữa run → cần checkpoint/retry |
| 1 container / 1 test | Isolation tuyệt đối | Overhead khởi tạo lớn |
| 1 container / N test | Throughput cao | State rò rỉ giữa test → flaky khó tìm |
| Fair queueing | Tenant nhỏ không bị bỏ rơi | Throughput tổng thấp hơn FIFO thuần |

---

## Biến thể hay bị hỏi: "một tenant gửi 10.000 test cùng lúc mà không ảnh hưởng tenant khác"

> Đây là bài **noisy neighbor**, không phải bài throughput. Nếu bạn trả lời bằng "scale thêm
> worker" là đã đọc sai đề — thêm worker chỉ làm tenant lớn chiếm nhanh hơn, tenant nhỏ vẫn chết đói.

### Nhận diện đúng vấn đề

```text
Câu hỏi NGHE như:   "làm sao chạy hết 10.000 test cho nhanh?"        <- throughput
Câu hỏi THẬT là:    "làm sao tenant B vẫn chạy được trong lúc đó?"   <- FAIRNESS

Với FIFO thuần: 10.000 test của A vào hàng trước -> test của B xếp sau cả 10.000 cái
                -> head-of-line blocking. B chờ hàng giờ dù chỉ submit 1 test.
```

### Bốn lớp phòng thủ — mỗi lớp chặn một kiểu hỏng khác nhau

| Lớp | Chặn cái gì | Cơ chế |
|---|---|---|
| **1. Quota cứng theo tenant** | Chặn ở cửa, trước khi vào hệ thống | `max_concurrent_runs` + `max_queued` theo gói. Vượt thì trả `429` kèm `Retry-After`, **không** âm thầm xếp hàng vô hạn |
| **2. Hàng đợi có trọng số** | Chặn head-of-line blocking | **Không** một hàng đợi chung. Mỗi tenant một hàng đợi ảo; scheduler rút vòng tròn có trọng số |
| **3. Priority + preemption** | Bảo vệ tenant trả tiền khi hết tài nguyên | K8s `PriorityClass`: enterprise > free. Hết node thì pod free bị **preempt**, test được đưa lại hàng đợi (không mất) |
| **4. Cô lập tài nguyên** | Chặn rò rỉ chéo | `ResourceQuota`/`LimitRange` theo namespace tenant; `NetworkPolicy` chặn cross-tenant; mỗi run một container riêng |

### Lớp 2 chi tiết — Weighted Fair Queueing / Deficit Round Robin

Đây là phần đáng vẽ lên bảng, vì nó trả lời trực tiếp vế "không ảnh hưởng tenant khác":

```text
                    +------------------------------------------+
  A submit 10.000   | queue[A]  ######################  10.000  |  weight 1
  B submit      5   | queue[B]  # 5                            |  weight 1
  C submit    200   | queue[C]  ### 200                        |  weight 3 (enterprise)
                    +----------------+-------------------------+
                                     v
                         Scheduler rút theo TRỌNG SỐ, không theo thứ tự đến
                         mỗi vòng:  A lấy 1 slot, B lấy 1 slot, C lấy 3 slot
                                     v
                            +--------------------+
                            |  Worker pool (N)   |
                            +--------------------+

-> B chờ tối đa ~1 vòng để có slot đầu tiên, KHÔNG phải chờ 10.000 test của A.
-> A vẫn dùng hết công suất rảnh khi B và C không có gì chạy (work-conserving).
```

**Deficit Round Robin (DRR)** là bản thực dụng của ý này khi test có **thời lượng khác nhau**: mỗi
tenant có một "quota tín dụng" cộng thêm mỗi vòng; test dài tiêu nhiều tín dụng hơn. Không có DRR
thì tenant toàn test 5 giây bị tenant toàn test 10 phút lấn, dù đếm theo *số* test là công bằng.

**Vì sao không dùng ưu tiên tuyệt đối (priority queue thuần):** tenant ưu tiên cao liên tục submit
sẽ làm tenant thấp **starve** vĩnh viễn. Trọng số đảm bảo mọi tenant đều tiến, chỉ khác tốc độ.

### Lớp bổ sung: chia shard bằng bin-packing, không chia đều

10.000 test chia cho 100 worker **không phải** là 100 test/worker:

```text
Chia đều 100 test/shard:
  shard 1: 100 test ngắn   -> xong sau 2 phút, worker ngồi không 18 phút
  shard 2: 100 test dài    -> xong sau 20 phút   <- cả run chờ shard này
  => wall-clock = 20 phút, ~50% công suất bị lãng phí

LPT (Longest Processing Time first) theo thời lượng LỊCH SỬ:
  sort test giảm dần theo p50 thời lượng lần chạy trước
  -> luôn ném test tiếp theo vào shard đang RẢNH NHẤT
  => wall-clock ~ 11 phút, chênh lệch giữa các shard < 10%
```

LPT là xấp xỉ 4/3 của tối ưu — đủ tốt, và chạy `O(n log n)`. Đây đúng là họ bài
**interval / load balancing** trong phần DSA (Meeting Rooms II, Task Scheduler).

**Điều kiện tiên quyết phải nói ra:** LPT cần **lịch sử thời lượng**. Test mới chưa có lịch sử thì
gán thời lượng trung vị của suite rồi cập nhật sau lần chạy đầu. Không có bước này thì LPT thoái
hoá về chia ngẫu nhiên.

### Trade-off (fairness)

| Lựa chọn | Được | Mất |
|---|---|---|
| Weighted fair queueing | Tenant nhỏ không bị bỏ rơi; latency dự đoán được | Throughput tổng thấp hơn FIFO thuần |
| Quota cứng ở cửa | Bảo vệ hệ thống chắc chắn nhất | Tenant lớn thấy bị "chặn" dù cluster đang rảnh -> cần quota **mềm** (burst khi rảnh) |
| Preemption | Tận dụng tối đa node | Test bị giết giữa chừng phải chạy lại; chỉ preempt test **ngắn/idempotent** |
| DRR thay vì round-robin đếm số | Công bằng theo **thời gian**, đúng bản chất | Cần đo thời lượng; phức tạp hơn |
| Warm pool | p50 latency thấp | Trả tiền cho node idle |

### Câu trả lời gọn (60 giây)

> *"Đây là bài fairness chứ không phải bài throughput — thêm worker không giải quyết được, vì với
> một hàng đợi FIFO chung thì 10.000 test của tenant A vẫn chặn đầu hàng của tenant B.*
>
> *Tôi làm bốn lớp. Một, quota cứng theo tenant ở cửa vào — vượt thì trả 429, không xếp hàng vô
> hạn. Hai, quan trọng nhất: không dùng một hàng đợi chung mà mỗi tenant một hàng đợi ảo, scheduler
> rút theo trọng số. Tenant B chờ tối đa một vòng để có slot đầu tiên. Ba, priority class và
> preemption để tenant trả tiền được bảo vệ khi hết node. Bốn, cô lập tài nguyên bằng namespace
> quota và network policy.*
>
> *Thêm một chi tiết ở tầng scheduling: tôi chia shard bằng bin-packing theo thời lượng lịch sử
> chứ không chia đều theo số lượng — chia đều thì shard toàn test dài quyết định wall-clock của cả
> run và phân nửa công suất bị lãng phí.*
>
> *Đánh đổi là throughput tổng thấp hơn FIFO thuần. Tôi chấp nhận, vì cái khách hàng cảm nhận được
> là độ trễ của chính họ, không phải throughput tổng của cluster."*
