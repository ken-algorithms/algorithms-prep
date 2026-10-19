# Bài 02 — Bên trong broker: segment, index, record batch, offset của consumer, metadata log, retention, compaction

> **Dùng cho:** **Katalon** và **NAB** — câu "vì sao Kafka nhanh", "Kafka lưu dữ liệu thế nào", "xoá một message được
> không", "consumer tìm offset thế nào" là câu đào sâu kinh điển sau khi bạn vẽ Kafka lên bảng.
>
> **Thời lượng:** đọc 45 phút · lab 15 phút · drill 10 phút
>
> **Lab:** [`scripts/k02-ben-trong-broker.sh`](lab/scripts/k02-ben-trong-broker.sh) · output thật:
> [`results/k02-ben-trong-broker.txt`](lab/results/k02-ben-trong-broker.txt)
>
> **Cần trước:** [bài 01](01-kafka-chay-docker-va-ket-noi.md) (cụm đang chạy). **Học tiếp:** [bài 03](03-kafka-replication-va-su-co.md).

```bash
cd on-bai/chuyen-sau/lab
docker compose -f docker/cluster.yml up -d --wait
scripts/k02-ben-trong-broker.sh          # ~4 phút (phần retention và compaction phải chờ broker quét)
```

---

## 0. Trọng tâm phỏng vấn

| # | Câu hỏi | Trả lời ngắn mà đúng tới tầng file |
|:-:|---|---|
| 1 | Kafka lưu một partition thế nào? | Một **thư mục** `<topic>-<partition>`, trong đó là các **segment**: mỗi segment = `.log` (record batch nối đuôi) + `.index` (offset → vị trí byte, thưa) + `.timeindex` (thời gian → offset). Tên file = offset đầu tiên của segment |
| 2 | Vì sao nhanh? | Ghi **nối đuôi** (sequential), đọc qua **page cache** của OS, gửi cho consumer bằng **zero-copy** (`sendfile`), và mọi thứ đi theo **batch** nén sẵn từ producer: broker không mở từng message |
| 3 | Consumer đọc từ offset X, broker làm gì? | Tìm segment có base offset lớn nhất ≤ X (cây/skip list), tìm nhị phân trong `.index` (được mmap) ra vị trí byte gần nhất ≤ X, quét tiếp `.log` một đoạn ngắn (≤ một batch) |
| 4 | Xoá một message được không? | Không xoá lẻ. **Retention** xoá **cả segment** đã cũ; **compaction** giữ bản mới nhất của mỗi key và **tombstone** (value null) để xoá key. Offset không bao giờ dùng lại |
| 5 | Offset của consumer nằm đâu? Metadata nằm đâu? | Đều là **log**: offset commit là record trong topic compacted `__consumer_offsets`; metadata cụm là log Raft `__cluster_metadata`; trạng thái transaction ở `__transaction_state` |
| 6 | Kafka có fsync từng message không? | **Không** (mặc định `log.flush.interval.messages` = `Long.MAX_VALUE`). Độ bền đến từ **replication** + `acks=all` (bài 03), không từ fsync |

---

## 1. Lý thuyết: một partition trên đĩa

```text
/var/lib/kafka/data/inside-0/                 <- một partition = một thư mục
├── 00000000000000000000.log        1,0 MB   <- segment 1: record batch nối đuôi, offset 0 .. 9410
├── 00000000000000000000.index      504 B    <- offset -> vị trí byte trong .log (THƯA)
├── 00000000000000000000.timeindex  588 B    <- timestamp -> offset
├── 00000000000000009411.log        1,0 MB   <- segment 2: tên = offset đầu tiên của nó
├── 00000000000000009411.index
├── 00000000000000009411.timeindex
├── 00000000000000009411.snapshot   102 B    <- trạng thái producer (idempotence) tại điểm cắt segment
├── 00000000000000028355.log        181 KB   <- segment ĐANG GHI (active)
├── 00000000000000028355.index      10 MB    <- cấp sẵn 10 MB, cắt về đúng cỡ khi segment đóng
├── leader-epoch-checkpoint                  <- epoch -> offset đầu tiên của epoch đó (bài 03)
└── partition.metadata                       <- topic id (UUID), chống nhầm topic xoá rồi tạo lại cùng tên
```

**Record batch (magic v2)** là đơn vị mọi tầng cùng hiểu: producer gom, nén, gửi một batch; broker ghi nguyên batch
xuống `.log`; consumer nhận nguyên batch rồi tự giải nén. Một batch có header cố định 61 byte:

| Trường | Ý nghĩa | Dùng cho |
|---|---|---|
| `baseOffset`, `lastOffsetDelta` | Offset của record đầu, độ lệch của record cuối | Record bên trong chỉ lưu *delta* (varint), nên offset tốn 1 byte/record |
| `partitionLeaderEpoch` | Epoch của leader lúc ghi | Cắt log đúng chỗ khi đổi leader (bài 03) |
| `producerId`, `producerEpoch`, `baseSequence` | Ai ghi, đời thứ mấy, số thứ tự | Idempotence: broker bỏ batch trùng |
| `attributes` | Kiểu nén, có thuộc transaction không, có phải record điều khiển (COMMIT/ABORT) | Transaction (bài 04) |
| `crc` | CRC32C của phần sau header | Phát hiện hỏng dữ liệu |
| `firstTimestamp`, `maxTimestamp` | | `.timeindex`, retention theo thời gian |

Hai hệ quả trực tiếp ra quyết định thiết kế:

1. **Chi phí mỗi record giảm theo cỡ batch.** Header 61 byte chia cho 1 record là 61 byte/record; chia cho 1.000
   record là 0,06 byte. Nén cũng tốt hơn khi batch to (từ điển chung). Bài 04 đo: batch to + nén cho thông lượng
   gấp nhiều lần.
2. **Broker không mở batch** (trừ khi phải nén lại vì `compression.type` của topic khác của producer). Nó kiểm CRC,
   gán offset, ghi nguyên khối. Đây là lý do một broker phục vụ được hàng trăm MB/s.

---

## 2. Real case: mở ổ đĩa của broker ra xem, từng bước

### Bước 1 — Tạo topic: segment rỗng đã có file index 10 MB

```text
Created topic inside.
Topic: inside  ...  Configs: min.insync.replicas=2,segment.bytes=1048576
    Topic: inside  Partition: 0  Leader: 1  Replicas: 1,2,3  Isr: 1,2,3

total 12
-rw-r--r-- 1 appuser appuser 10485760 .. 00000000000000000000.index
-rw-r--r-- 1 appuser appuser       94 .. 00000000000000000000.log
-rw-r--r-- 1 appuser appuser 10485756 .. 00000000000000000000.timeindex
-rw-r--r-- 1 appuser appuser        8 .. leader-epoch-checkpoint
-rw-r--r-- 1 appuser appuser       43 .. partition.metadata
```

- `segment.bytes=1048576` (1 MiB) là **giá trị nhỏ nhất Kafka 4.x cho phép**; thử 100.000 thì broker từ chối:
  `Invalid value 100000 for configuration segment.bytes: Value must be at least 1048576`. Mặc định là 1 GiB.
- `.index` và `.timeindex` của segment **đang ghi** được cấp sẵn **10 MB** (`segment.index.bytes`) và **mmap** vào bộ
  nhớ, để ghi một mục index chỉ là ghi vào RAM. `total 12` cho thấy chúng là *sparse file*: 10 MB trên danh nghĩa,
  gần như không chiếm đĩa thật.

### Bước 2 — Ba record nhỏ: đọc thẳng file `.log`

```bash
kafka-dump-log.sh --files /var/lib/kafka/data/inside-0/00000000000000000000.log --print-data-log
```

```text
baseOffset: 0 lastOffset: 2 count: 3 baseSequence: 0 lastSequence: 2 producerId: 1000 producerEpoch: 0
partitionLeaderEpoch: 0 isTransactional: false isControl: false position: 0 CreateTime: 1792396013527
size: 94 magic: 2 compresscodec: none crc: 3432338532 isvalid: true
| offset: 0 CreateTime: 1792396013475 keySize: 2 valueSize: 2 sequence: 0 headerKeys: [] key: k1 payload: v1
| offset: 1 CreateTime: 1792396013527 keySize: 2 valueSize: 2 sequence: 1 headerKeys: [] key: k2 payload: v2
| offset: 2 CreateTime: 1792396013527 keySize: 2 valueSize: 2 sequence: 2 headerKeys: [] key: k1 payload: v3
```

- **Ba record = một batch 94 byte**: 61 byte header + 3 × ~11 byte. Payload thật chỉ có 12 byte (`k1 v1 k2 v2 k1 v3`).
  Đây là ví dụ thu nhỏ của bài toán "mỗi event một message" ở 10 triệu event/giây (bài 06): overhead lớn hơn dữ liệu.
- **`producerId: 1000`, `baseSequence: 0..2`**: console producer bật idempotence (mặc định từ Kafka 3.0). Producer xin
  một *producer id* lúc khởi động; mỗi batch mang số thứ tự tăng dần theo từng partition. Bước 7 cho thấy broker
  nhớ các số này ở đâu.
- **`partitionLeaderEpoch: 0`**: batch được ghi khi broker 1 làm leader ở epoch 0. Bài 03 dùng chính trường này để
  hiểu vì sao broker cũ tự cắt bỏ dữ liệu sau khi quay lại.

### Bước 3 — Ghi 3 MB: segment cuộn (roll)

```text
30000 records sent, 8547.0 records/sec (0.82 MB/sec), 1313.72 ms avg latency ...
-rw-r--r-- 1 appuser appuser      504 .. 00000000000000000000.index
-rw-r--r-- 1 appuser appuser  1034782 .. 00000000000000000000.log
-rw-r--r-- 1 appuser appuser      588 .. 00000000000000000000.timeindex
-rw-r--r-- 1 appuser appuser      504 .. 00000000000000009411.index
-rw-r--r-- 1 appuser appuser  1041728 .. 00000000000000009411.log
-rw-r--r-- 1 appuser appuser      102 .. 00000000000000009411.snapshot
-rw-r--r-- 1 appuser appuser      552 .. 00000000000000009411.timeindex
-rw-r--r-- 1 appuser appuser      504 .. 00000000000000018883.index
-rw-r--r-- 1 appuser appuser  1041728 .. 00000000000000018883.log
-rw-r--r-- 1 appuser appuser 10485760 .. 00000000000000028355.index      <- segment đang ghi
-rw-r--r-- 1 appuser appuser   181288 .. 00000000000000028355.log
```

- Segment đầy 1 MiB thì **cuộn**: đóng segment cũ, mở segment mới tên là offset kế tiếp (9411, 18883, 28355).
- Segment đã đóng thì **index được cắt về đúng cỡ**: 504 byte = 63 mục × 8 byte (4 byte offset tương đối + 4 byte vị
  trí). `.timeindex` mỗi mục 12 byte (8 byte timestamp + 4 byte offset): 588 byte = 49 mục.
- Một file `.snapshot` sinh ra **ở mỗi lần cuộn**: ảnh chụp trạng thái producer tại điểm đó (bước 7).
- Đừng đọc con số 8.547 record/giây như hiệu năng Kafka: đây là perf-test chạy **trong** container trên máy 4 vCPU đang
  chạy 7 JVM, với `acks=all`, batch 16 KB. Bài 04 đo cùng cụm này lên hàng trăm nghìn record/giây khi chỉnh batch.

### Bước 4 — Index thưa: tìm offset X thế nào

```text
offset: 276 position: 13951
offset: 424 position: 30228
offset: 530 position: 46505
offset: 678 position: 58162
...
so muc index trong segment dau: 63
```

Index **thưa**: broker chỉ thêm một mục khi đã ghi thêm ≥ `index.interval.bytes` (4 KB), và chỉ ở ranh giới batch.
Batch ở đây ~16 KB nên các mục cách nhau ~16 KB, tức ~150 record. Một mục ghi **offset cuối của một batch** và **vị trí
byte của batch đó** trong `.log`.

Consumer `fetch(offset = 500)`:

```text
1. Chọn segment:  segment có base offset lớn nhất <= 500           -> 00000000000000000000  (skip list theo base offset)
2. Tìm trong .index (mmap, nhị phân): mục lớn nhất có offset <= 500 -> (424, vị trí 30228)
3. Quét .log từ byte 30228 tới batch chứa offset 500               -> đọc tối đa ~1 batch
4. Gửi từ vị trí đó, theo nguyên batch, bằng sendfile                -> không copy qua JVM heap
```

Chi phí: O(log số segment) + O(log số mục index) + một đoạn quét ngắn. **Không phụ thuộc kích thước topic.** Đọc từ
offset 0 hay offset một tỉ đều như nhau. Cái tốn kém thật là khi phần cần đọc **không còn trong page cache**: phải đọc
đĩa (consumer đọc lại dữ liệu cũ, "catch-up read").

### Bước 5 — Time index: `offsetsForTimes`

```text
timestamp: 1792396021171 offset: 276
timestamp: 1792396021177 offset: 424
...
```

`.timeindex` cho câu hỏi "offset đầu tiên có timestamp ≥ T": dùng khi consumer muốn *tua lại về 9 giờ sáng*
(`offsetsForTimes`), và khi xoá theo thời gian (retention so sánh `maxTimestamp` của segment). Bài 08 dùng chính API
này để aggregator đọc lại 15 phút gần nhất khi khởi động.

### Bước 6 — Hai file nhỏ mà quan trọng

```text
--- leader-epoch-checkpoint
0          <- phiên bản định dạng
1          <- số mục
0 0        <- epoch 0 bắt đầu ở offset 0
--- partition.metadata
version: 0
topic_id: eXsO6Ej3Q1yO2uK2cyjoRw
```

- `leader-epoch-checkpoint` ghi **mỗi epoch của leader bắt đầu ở offset nào**. Khi một replica quay lại sau sự cố, nó
  hỏi leader mới "epoch cuối của tôi kết thúc ở offset nào bên anh?" rồi **cắt** phần log thừa (bài 03 có log thật:
  `Truncating partition pay1-0 ... due to leader epoch and offset`). Trước KIP-101, Kafka cắt theo high watermark và
  có thể mất hoặc lệch dữ liệu giữa các replica.
- `partition.metadata` giữ **topic id**: xoá topic `orders` rồi tạo lại `orders` thì id khác, broker không nhầm thư
  mục cũ với topic mới.

### Bước 7 — Producer state snapshot: broker nhớ sequence để chặn bản ghi trùng

```text
Dumping .../00000000000000009411.snapshot
producerId: 0    producerEpoch: 0 ... firstSequence: 9260 lastSequence: 9407 lastOffset: 9410 offsetDelta: 147
producerId: 1000 producerEpoch: 0 ... firstSequence: 0    lastSequence: 2    lastOffset: 2    offsetDelta: 2
```

Mỗi partition giữ trong RAM, với từng `producerId`, epoch và metadata của **5 batch gần nhất**. Producer gửi lại một
batch (retry sau timeout) có `(producerId, epoch, sequence)` trùng một trong 5 batch đó thì broker **trả lại offset cũ,
không ghi lần hai**. Đây là lý do idempotence yêu cầu `max.in.flight.requests.per.connection ≤ 5`. File `.snapshot` là
ảnh chụp trạng thái đó ở mỗi lần cuộn segment, để broker khởi động lại không phải đọc lại cả log mới dựng được.

> Bài 06–08 dùng **đúng ý tưởng này** cho client HTTP: `(clientId, epoch, seq)` + cửa sổ nhớ các seq gần nhất. Nói
> được "tôi chép thiết kế idempotent producer của Kafka" là điểm cộng.

### Bước 8 — Đầu và cuối của log

```text
inside:0:0        <- --time -2: earliest (log start offset)
inside:0:30003    <- --time -1: latest (log end offset = offset kế tiếp sẽ được cấp)
```

### Bước 9 — Offset của consumer cũng chỉ là record

```text
group 'g-inside' -> partition abs(hashCode) % 50 = 6 cua __consumer_offsets
leader cua __consumer_offsets-6 (group coordinator) la broker 2
| offset: 0 ... key: {"type":"2","data":{"group":"g-inside"}} payload: {...,"generation":0,"members":[]}}
| offset: 1 ... key: {"type":"2","data":{"group":"g-inside"}} payload: {"protocolType":"consumer","generation":1,"protocol":"range",...}
| offset: 2 ... key: {"type":"1","data":{"group":"g-inside","topic":"inside","partition":0}} payload: {"offset":5,"leaderEpoch":-1,...}
| offset: 3 ... key: {"type":"2","data":{"group":"g-inside"}} payload: {"generation":2,"members":[]}}

GROUP     TOPIC   PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
g-inside  inside  0          5               30003           29998
```

- **Group coordinator** của một group là leader của partition `abs(group.hashCode()) % 50` trong `__consumer_offsets`
  (50 = `offsets.topic.num.partitions`). Mọi JoinGroup, heartbeat, commit offset của group đó đi tới broker này.
- **Commit offset = ghi một record** key `(group, topic, partition)`, value `offset 5`. Record type 2 là metadata của
  group (thế hệ, thành viên, chiến lược chia). Topic này **compacted**: chỉ giữ commit mới nhất mỗi key.
- Commit offset **ghi xuống log có replication** như mọi dữ liệu khác, nên commit thường xuyên (mỗi poll) tốn một lần
  ghi nhỏ; commit theo lô là đủ.

### Bước 10 — Metadata của cả cụm cũng là một log

```text
     51 "type":"PARTITION_RECORD"
      5 "type":"CONFIG_RECORD"
      4 "type":"FEATURE_LEVEL_RECORD"
      3 "type":"REGISTER_BROKER_RECORD"
      3 "type":"BROKER_REGISTRATION_CHANGE_RECORD"
      2 "type":"TOPIC_RECORD"
      2 "type":"PRODUCER_IDS_RECORD"
{"type":"TOPIC_RECORD","data":{"name":"inside","topicId":"eXsO6Ej3Q1yO2uK2cyjoRw"}
{"type":"PARTITION_RECORD","data":{"partitionId":0,"topicId":"eXsO6Ej3Q1yO2uK2cyjoRw","replicas":[1,2,3],"isr":[1,2,3],
 "leader":1,"leaderEpoch":0,"partitionEpoch":0,...}
```

Tạo topic = controller ghi **một `TopicRecord` + mỗi partition một `PartitionRecord`** vào `__cluster_metadata`.
ISR co lại = một `PartitionChangeRecord`. Broker đăng ký = `RegisterBrokerRecord`. `PRODUCER_IDS_RECORD` là các
**dải producer id** (mỗi dải 1.000 id) controller cấp cho broker; bài 03 thấy hậu quả khi broker hết dải mà controller
mất quorum. Toàn bộ trạng thái cụm là kết quả *phát lại* log này. Đây là ý tưởng event sourcing, áp dụng cho chính
Kafka.

### Bước 11 — Retention: xoá theo segment, không theo message

Topic `short`: `retention.ms=15000`, `segment.ms=5000`, broker quét mỗi 5 giây (lab đặt
`log.retention.check.interval.ms=5000`; mặc định 5 phút).

```text
--- sau 4 lan ghi cach nhau 6 giay (segment da roll):
00000000000000000000.log.deleted
00000000000000001000.log.deleted
00000000000000002000.log.deleted
00000000000000003000.log
short:0:3000                         <- log start offset
--- 25 giay sau: segment cu het han bi xoa CA FILE, log-start-offset nhay len
...
00000000000000003000.log.deleted
00000000000000004000.log
short:0:4000                         <- earliest
short:0:4000                         <- latest: log RỖNG nhưng offset vẫn tiếp từ 4000
[07:48:15,550] INFO [UnifiedLog partition=short-0] Deleting segment LogSegment(baseOffset=0, size=109979,
  largestRecordTimestamp=1792396075972) due to log retention time 15000ms breach based on the largest record timestamp
```

- Đơn vị xoá là **segment**, khi `largestRecordTimestamp` của nó đã quá `retention.ms`. Một record cũ nằm chung segment
  với record mới thì **sống tới khi cả segment hết hạn**. Muốn retention chính xác hơn thì segment nhỏ hơn
  (`segment.ms`), đổi lại nhiều file hơn.
- Segment bị đổi tên thành `.deleted` rồi mới xoá thật sau `file.delete.delay.ms` (60 giây), để không xoá file mà một
  luồng đọc đang mở.
- **Offset không bao giờ dùng lại**: log đã rỗng (earliest = latest = 4000) nhưng record kế tiếp vẫn là 4000. Consumer
  đang ở offset 1500 sẽ nhận `OFFSET_OUT_OF_RANGE` và áp `auto.offset.reset` (`earliest` → nhảy tới 4000, tức là **mất**
  1500–3999 mà không ai báo). Đây là lý do phải giám sát lag và để retention dài hơn thời gian sự cố tệ nhất của consumer.

### Bước 12 — Compaction: giữ bản mới nhất của mỗi key

Topic `registry` (`cleanup.policy=compact`, `min.cleanable.dirty.ratio=0.01`, `delete.retention.ms=5000`):

```text
--- (a) vua ghi xong: moi phien ban con nguyen, offset 5 la tombstone
Offset:0  checkout_v2  valid@v1
Offset:1  search       valid@v1
Offset:2  checkout_v2  valid@v2
Offset:3  legacy_api   valid@v1
Offset:4  search       valid@v2
Offset:5  legacy_api   null
--- (b) cleaner chay xong lan 1: con ban moi nhat moi key, tombstone van giu (delete.retention.ms)
Offset:2  checkout_v2  valid@v2
Offset:4  search       valid@v2
Offset:5  legacy_api   null
Offset:6  roll1        x
--- (c) lan 2, qua delete.retention.ms: tombstone bien mat, key legacy_api khong con dau vet
Offset:2  checkout_v2  valid@v2
Offset:4  search       valid@v2
Offset:6  roll1        x
Offset:7  roll2        x
Log cleaner thread 0 cleaned log registry-0 (dirty section = [0, 6])
Log cleaner thread 0 cleaned log registry-0 (dirty section = [6, 7])
```

Năm điều rút ra:

1. **Offset giữ nguyên, chỉ có lỗ hổng** (0, 1, 3 biến mất; 2, 4 còn). Thứ tự giữa các key được giữ.
2. **Segment đang ghi không bao giờ bị compact.** Lab phải ghi `roll1` sau `segment.ms` để đóng segment cũ thì cleaner
   mới đụng tới.
3. **Tombstone (`value = null`) là cách xoá key.** Nó được giữ thêm `delete.retention.ms` sau lần dọn đầu, để consumer
   đang đọc chậm kịp thấy "key này đã bị xoá"; hết hạn thì bị dọn ở lần sau (c).
4. **Không có hạn thời gian cứng cho việc dọn.** Cleaner chạy khi tỉ lệ phần "bẩn" vượt `min.cleanable.dirty.ratio`
   (mặc định 0,5) và chờ `log.cleaner.backoff.ms`; muốn chắc chắn bản cũ biến mất trước một thời điểm thì dùng
   `max.compaction.lag.ms`.
5. **Dùng cho trạng thái "bản mới nhất theo key"**: `__consumer_offsets`, changelog của Kafka Streams, cấu hình,
   **registry key hợp lệ của bài đếm sự kiện** ([bài 07](07-dem-su-kien-code-ingest.md)): node mới khởi động đọc topic
   compacted từ đầu là có đúng tập key hiện tại mà không phải đọc lại cả lịch sử.

---

## 3. Vì sao Kafka nhanh — bốn lý do, kèm bằng chứng

| Lý do | Cơ chế | Thấy ở đâu trong lab |
|---|---|---|
| Ghi nối đuôi | Mỗi partition chỉ append vào segment đang ghi: ghi tuần tự, không seek | Bước 3: file `.log` lớn dần, cuộn theo cỡ |
| Page cache | Broker không tự cache; OS giữ phần đuôi log trong RAM. Consumer theo kịp thì đọc từ RAM | Heap broker chỉ 512 MB trong lab mà vẫn phục vụ hàng trăm MB |
| Zero-copy | Gửi cho consumer bằng `FileChannel.transferTo` → `sendfile`: dữ liệu đi từ page cache ra socket, không qua heap JVM. **Mất** khi bật TLS (phải mã hoá ở user space) | — |
| Batch + nén từ đầu tới cuối | Producer nén cả batch; broker ghi nguyên khối; consumer giải nén | Bước 2: batch header 61 byte; bài 04: batch to + nén cho thông lượng gấp 4–10 lần |

> Thêm một lý do ít người nói: **không fsync từng message**. Ghi chỉ vào page cache rồi trả lời; dữ liệu an toàn vì đã
> nằm trên RAM của **2–3 broker khác nhau** (replication). Bài 03 cho thấy log recovery khi broker bị `kill -9`:
> `Recovering unflushed segment 0`. Đánh đổi: mất điện **cùng lúc cả 3 broker** (cùng rack, cùng AZ) có thể mất dữ
> liệu đã xác nhận. Vì vậy replica phải ở các AZ khác nhau (`broker.rack`).

---

## 4. Vẽ trên bảng trắng

```text
topic "orders", partition 3  ─►  thư mục orders-3/
  segment [0 .. 9410]      .log  .index(mmap)  .timeindex      ┐ đã đóng: chỉ đọc; retention xoá CẢ segment
  segment [9411 .. 18882]  .log  .index        .timeindex      ┘ compaction viết lại thành segment sạch
  segment [18883 .. ]      .log  ← producer append (batch nguyên khối, CRC, offset delta)
                              ▲
  fetch(offset X): skip list chọn segment → nhị phân trên .index → quét ≤ 1 batch → sendfile ra socket

__consumer_offsets  (compacted)  : (group, topic, partition) -> offset đã commit
__transaction_state (compacted)  : transactional.id -> trạng thái transaction
__cluster_metadata  (Raft log)   : TopicRecord, PartitionRecord, RegisterBrokerRecord, ProducerIdsRecord...
```

---

## 5. Drill (10 phút): "Consumer muốn đọc lại từ 9 giờ sáng hôm qua"

Viết các bước và nói chi phí từng bước. Tiêu chí:

- [ ] Client gọi `offsetsForTimes(T)` → broker tra `.timeindex` của các segment → offset đầu tiên có timestamp ≥ T.
- [ ] Nếu T cũ hơn log start offset (đã bị retention xoá) thì trả về `null`: phải xử lý.
- [ ] `seek(partition, offset)` rồi `poll`: broker dùng `.index` tìm vị trí byte, quét ≤ 1 batch.
- [ ] Phần đó gần như chắc chắn **không** còn trong page cache: đọc đĩa, có thể làm chậm consumer khác trên cùng
      broker (đẩy phần nóng ra khỏi page cache). Giải pháp: giới hạn tốc độ đọc lại, hoặc tiered storage cho dữ liệu cũ.
- [ ] Timestamp mặc định là `CreateTime` do producer gán; client đặt đồng hồ sai thì T không chính xác
      (`log.message.timestamp.type=LogAppendTime` nếu cần thời gian của broker).

---

## 6. Follow-up hay gặp

<details>
<summary><b>Q1. Cần xoá dữ liệu một khách hàng (GDPR) trong topic thì làm sao?</b></summary>

Không có "xoá record". Ba cách: (1) topic **compacted** theo `customerId`, ghi tombstone; chờ compaction (đặt
`max.compaction.lag.ms` để có hạn chót). (2) **Crypto-shredding**: mã hoá dữ liệu mỗi khách bằng một khoá riêng, xoá
khoá là dữ liệu không đọc được nữa. (3) Retention ngắn + đồng bộ sang kho khác có hỗ trợ xoá. Nói rõ: backup, consumer
đã đọc, kho phía sau cũng phải xoá theo.
</details>

<details>
<summary><b>Q2. Segment to hay nhỏ thì tốt?</b></summary>

To (1 GB mặc định): ít file, ít mmap, ít lần cuộn. Nhỏ: retention và compaction chính xác hơn (đơn vị xoá nhỏ hơn),
nhưng nhiều file mở hơn (mỗi segment 3–4 file). Với topic retention ngắn (vài giờ) hoặc compacted cần dọn nhanh thì
giảm `segment.ms`/`segment.bytes`; còn lại để mặc định.
</details>

<details>
<summary><b>Q3. Tiered storage là gì, khi nào dùng?</b></summary>

KIP-405 (GA từ Kafka 3.9): segment đã đóng được chuyển lên object storage (S3), broker chỉ giữ phần đuôi trên ổ local.
Dùng khi cần retention dài (tuần, tháng) mà không muốn mua đĩa cho mọi broker × RF. Đọc dữ liệu cũ thì chậm hơn (đi
qua object storage). Thêm/bớt broker cũng nhanh hơn vì ít dữ liệu local phải chép.
</details>

<details>
<summary><b>Q4. Vì sao không fsync mà vẫn dám nói "bền"?</b></summary>

Vì một record chỉ được xác nhận (`acks=all`) khi đã nằm trong bộ nhớ của **mọi replica trong ISR** (ít nhất
`min.insync.replicas` máy). Mất một máy không mất dữ liệu; mất cùng lúc mọi máy trong ISR (cùng nguồn điện) mới mất.
fsync từng message làm thông lượng giảm hàng chục lần. Ai cần thêm thì đặt `flush.messages`/`flush.ms` cho topic đó,
và đo cái giá.
</details>

<details>
<summary><b>Q5. Record lớn (vài MB) thì sao?</b></summary>

Mặc định `message.max.bytes` ~1 MB (theo batch). Tăng được, nhưng record to làm batch to, tốn bộ nhớ ở cả producer,
broker, consumer (`max.partition.fetch.bytes`), và làm replication chậm. Cách chuẩn: lưu payload lớn ở object storage,
gửi qua Kafka **con trỏ** (claim-check pattern).
</details>

---

## 7. Checklist

- [ ] Kể được các file trong thư mục partition và mỗi file để làm gì.
- [ ] Đọc được một dòng `kafka-dump-log` và chỉ ra producerId, sequence, leader epoch.
- [ ] Giải thích được tìm offset X: skip list → index nhị phân → quét ≤ 1 batch; vì sao index thưa.
- [ ] Biết consumer offset, transaction state, metadata cụm đều là log.
- [ ] Biết retention xoá theo segment, offset không dùng lại, hệ quả `OFFSET_OUT_OF_RANGE`.
- [ ] Biết compaction giữ bản mới nhất, tombstone, segment đang ghi không bị dọn.
- [ ] Nói được bốn lý do Kafka nhanh và cái giá của việc không fsync.
