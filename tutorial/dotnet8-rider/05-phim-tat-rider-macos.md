# Bài 05 — Phím tắt Rider trên MacBook: gỡ xung đột macOS, 15 phím sống còn, bảng đầy đủ theo nhóm, bài luyện ngón tay

> **Bạn sẽ làm được:** dùng Rider gần như không cần chuột: tìm file/class/lệnh, nhảy qua lại trong code, sửa và
> refactor, build/run/debug, git. Trước đó gỡ 4 xung đột phím mà macOS gây ra cho mọi IDE của JetBrains.
>
> **Thời lượng:** đọc 20 phút · luyện 15 phút mỗi ngày trong tuần đầu
>
> **Keymap của bài:** **IntelliJ (macOS)**, chọn ở [bài 01 §4.2](01-cai-dat-macos.md). Dùng keymap khác thì tra phím
> của máy bạn bằng **⇧⌘A** ([§5](#5-tự-tra-phím-tắt-không-cần-bảng-này)).
>
> **Trước đó:** [Bài 04 — Debug](04-debug-trong-rider.md) · **Tiếp theo:** [Bài 06 — ASP.NET Core Web API](06-web-api-asp-net-core.md)

---

## 0. Đọc ký hiệu phím trên Mac

| Ký hiệu | Phím | Ký hiệu | Phím |
|---|---|---|---|
| **⌘** | Command | **⏎** | Return (Enter) |
| **⌥** | Option (Alt) | **⌫** | Delete (xoá lùi) |
| **⌃** | Control | **⌦** | Forward Delete = **fn + ⌫** trên MacBook |
| **⇧** | Shift | **Esc** | Escape |
| **fn** / **🌐** | Function / Globe | **F1–F12** | hàng trên cùng, xem [§1.1](#11-hàng-f1f12-đang-là-phím-âm-lượng-độ-sáng) |

Thứ tự viết chuẩn của Apple là ⌃ ⌥ ⇧ ⌘ rồi tới phím chính, ví dụ **⌥⇧⌘F8**: giữ Option + Shift + Command rồi bấm F8.
**⇧⇧** nghĩa là bấm Shift **hai lần liền**.

---

## 1. Gỡ xung đột với macOS trước (10 phút, làm một lần)

### 1.1 Hàng F1–F12 đang là phím âm lượng, độ sáng

Debug dùng F7, F8, F9 liên tục. Trên MacBook, bấm F8 mặc định là "play/pause nhạc".

- **Cách 1 (khuyên dùng):** **System Settings → Keyboard → Keyboard Shortcuts… → Function Keys** → bật
  **Use F1, F2, etc. keys as standard function keys**. Muốn chỉnh âm lượng thì giữ **fn**.
- **Cách 2:** giữ nguyên, mỗi lần bấm **fn + F8**.
- Máy có Touch Bar: cùng mục đó có thể chọn hiện phím F cho riêng ứng dụng Rider.

### 1.2 ⌃Space đang dùng để đổi bộ gõ (trùng gợi ý code)

macOS dùng **⌃Space** để chuyển nguồn nhập (ABC ↔ Tiếng Việt). Rider dùng **⌃Space** cho **Basic Completion**.
Người dùng bộ gõ tiếng Việt gần như chắc chắn gặp.

**System Settings → Keyboard → Keyboard Shortcuts… → Input Sources** → bỏ tick
**Select the previous input source** (đổi bộ gõ bằng phím 🌐/fn hoặc gán phím khác).

### 1.3 ⇧⌘A mở Terminal tra `man` thay vì Find Action

macOS có dịch vụ **Search man Page Index in Terminal** gán sẵn **⇧⌘A**. Bấm Find Action mà Terminal bật lên là do nó.

**System Settings → Keyboard → Keyboard Shortcuts… → Services → Text** → bỏ tick **Search man Page Index in Terminal**.

### 1.4 ⌃← / ⌃→ chuyển màn hình (Spaces) thay vì chuyển tab

Trong keymap, chuyển tab editor có hai phím: **⇧⌘[ / ⇧⌘]** và **⌃← / ⌃→**. Cặp sau trùng **Mission Control →
Move left/right a space**. Dùng **⇧⌘[ / ⇧⌘]** cho khỏi đụng, hoặc tắt phím của Mission Control trong
**Keyboard Shortcuts… → Mission Control**.

### 1.5 Bộ gõ tiếng Việt khi code

Chuyển sang **ABC** khi code, hoặc cho EVKey/OpenKey nhớ chế độ tiếng Anh với Rider ([bài 01 §6.4](01-cai-dat-macos.md)).

---

## 2. Mười lăm phím sống còn (thuộc trong tuần đầu)

| # | Phím | Làm gì | Khi nào |
|:-:|---|---|---|
| 1 | **⇧⇧** | Search Everywhere: file, class, method, lệnh, setting | Không nhớ gì thì bấm cái này |
| 2 | **⇧⌘A** | Find Action: tìm **lệnh** theo tên, hiện kèm phím tắt | "Rider có lệnh … không?" |
| 3 | **⌥⏎** | Gợi ý sửa / hành động tại chỗ (import, tạo method, đổi kiểu…) | Thấy gạch đỏ/vàng |
| 4 | **⌘O** | Go to Class/Type | Mở `OrderService` |
| 5 | **⇧⌘O** | Go to File | Mở `appsettings.json` |
| 6 | **⌘E** | Recent Files | Quay lại file vừa sửa |
| 7 | **⌘B** hoặc **⌘Click** | Go to Declaration | Xem định nghĩa |
| 8 | **⌥⌘B** | Go to Implementation | Từ interface nhảy tới class cài đặt |
| 9 | **⌥F7** | Find Usages | Ai đang gọi method này |
| 10 | **⌘[ / ⌘]** | Back / Forward | Quay lại chỗ vừa đứng |
| 11 | **⇧F6** | Rename (đổi tên an toàn mọi nơi) | Đặt lại tên |
| 12 | **⌃T** | Refactor This: menu mọi kiểu refactor | Không nhớ phím refactor |
| 13 | **⌘/** | Comment/uncomment dòng | |
| 14 | **⌃R / ⌃D** | Run / Debug cấu hình đang chọn | |
| 15 | **⌘F8, F8, F7, ⌥F8** | Breakpoint, Step Over, Step Into, Evaluate | Bài 04 |

---

## 3. Bảng đầy đủ theo nhóm (keymap IntelliJ macOS)

Các phím không có dấu † đối chiếu từ file keymap gốc của nền tảng IntelliJ ([§10](#10-ranh-giới-trung-thực)).

### 3.1 Tìm kiếm và điều hướng

| Phím | Lệnh | Ghi chú |
|---|---|---|
| ⇧⇧ | Search Everywhere | Tab để lọc theo All / Types / Files / Symbols / Actions |
| ⇧⌘A | Find Action | |
| ⌘O | Go to Type | gõ chữ hoa đầu: `OCH` tìm ra `OrderCreatedHandler` |
| ⇧⌘O | Go to File | gõ `:25` sau tên file để tới dòng 25 |
| ⌥⌘O | Go to Symbol | method, property, field |
| ⌘L | Go to Line | |
| ⌘E | Recent Files | |
| ⇧⌘E | Recent Locations | những **đoạn code** vừa xem, không chỉ file |
| ⌃Tab | Switcher | giữ ⌃, bấm Tab liên tục để chọn file/cửa sổ |
| ⌘B, ⌘Click | Go to Declaration | |
| ⌥⌘B, ⌥⌘Click | Go to Implementation | |
| ⇧⌘B, ⇧⌘Click | Go to Type Declaration | từ biến nhảy tới class của kiểu đó |
| ⌘U | Go to Super Method | |
| ⇧⌘T | Go to Test | qua lại giữa class và test của nó |
| ⌃⌘↑ | Go to Related Symbol | |
| ⌥F7 | Find Usages | kết quả ở cửa sổ Find (⌘3) |
| ⌥⌘F7 | Show Usages | popup ngay tại chỗ |
| ⌘F12 | File Structure | danh sách member của file, gõ để lọc |
| ⌃H | Type Hierarchy | |
| ⌃⌥H | Call Hierarchy | |
| ⌘[ , ⌥⌘← | Back | |
| ⌘] , ⌥⌘→ | Forward | |
| F2 / ⇧F2 | Lỗi/cảnh báo kế tiếp / trước | |
| ⌃⇧↓ / ⌃⇧↑ | Method kế tiếp / trước | |
| ⌥F1 | Select In… | rồi chọn Explorer để thấy file trong cây |
| F3 / ⌘F3 | Bật bookmark / xem bookmark | |
| ⌘F / ⌘R | Tìm / thay thế trong file | ⌘G, ⇧⌘G: kết quả kế/trước |
| ⇧⌘F / ⇧⌘R | Tìm / thay thế trong cả solution | |

### 3.2 Soạn code

| Phím | Lệnh | Ghi chú |
|---|---|---|
| ⌃Space | Basic Completion | sau khi gỡ xung đột [§1.2](#12-space-đang-dùng-để-đổi-bộ-gõ-trùng-gợi-ý-code) |
| ⌃⇧Space | Smart Completion | chỉ gợi ý đúng kiểu cần |
| ⌥⏎ | Show Context Actions | |
| ⌘N, ⌃⏎ | Generate | constructor, property, Equals… Trong Explorer: tạo file mới |
| ⌥⌘T | Surround With | bọc `try/catch`, `if`, `using` |
| ⌘J | Insert Live Template | danh sách template, [§6](#6-live-template-và-postfix-template-gõ-ít-ra-nhiều) |
| ⇧⌘⏎ | Complete Current Statement | thêm `;`, `{}` cho xong câu |
| ⇧⏎ | Dòng mới bên dưới (con trỏ ở giữa dòng vẫn được) | |
| ⌘P | Parameter Info | tham số của method đang gọi |
| F1, ⌃J | Quick Documentation | |
| ⌥Space, ⌘Y | Quick Definition | xem nhanh code mà không mở file |
| ⌃⇧P | Type Info | kiểu của biểu thức đang chọn (`var` là gì) |
| ⌘F1 | Mô tả lỗi/cảnh báo tại con trỏ | |
| ⌘/ | Comment dòng | |
| ⌥⌘/ | Comment khối | |
| ⌘D | Nhân đôi dòng/vùng chọn | |
| ⌘⌫ | Xoá dòng | |
| ⌥⇧↑ / ⌥⇧↓ | Di chuyển dòng | |
| ⇧⌘↑ / ⇧⌘↓ | Di chuyển cả câu lệnh/method | |
| ⌃⇧J | Nối dòng | |
| ⇧⌘U | Đổi hoa/thường | |
| ⌥⌘L | Reformat Code | |
| ⌃⌥O | Optimize Imports | dọn `using` thừa |
| ⌃⌥I | Auto-indent dòng | |
| ⌘+ / ⌘- | Mở/gập khối code | ⇧⌘+ / ⇧⌘-: tất cả |
| ⇧⌘V | Paste from History | chọn từ các lần copy trước |
| ⌘Z / ⇧⌘Z | Undo / Redo | |

### 3.3 Chọn và nhiều con trỏ

| Phím | Lệnh |
|---|---|
| ⌥↑ / ⌥↓ | Mở rộng / thu hẹp vùng chọn theo cú pháp (từ → biểu thức → câu → khối → method) |
| ⌃G | Chọn thêm lần xuất hiện kế tiếp của chữ đang chọn (mỗi chỗ một con trỏ) |
| ⌃⇧G | Bỏ lần vừa chọn |
| ⌃⌘G | Chọn mọi lần xuất hiện trong file |
| ⌥⇧Click | Thêm/bớt con trỏ bằng chuột |
| ⌥⇧G | Mỗi dòng đang chọn một con trỏ |
| ⇧⌘8 | Chế độ chọn theo cột |
| ⌥← / ⌥→ | Nhảy theo từ (giữ ⇧ để chọn) |
| ⌘← / ⌘→ | Đầu / cuối dòng |
| ⌥⌘[ / ⌥⌘] | Đầu / cuối khối code |

### 3.4 Refactor

| Phím | Lệnh |
|---|---|
| ⌃T | Refactor This (menu) |
| ⇧F6 | Rename |
| ⌘F6 | Change Signature |
| ⌥⌘M | Extract Method |
| ⌥⌘V | Introduce Variable |
| ⌥⌘F | Introduce Field |
| ⌥⌘P | Introduce Parameter |
| ⌥⌘C | Introduce Constant |
| ⌥⌘N | Inline |
| F6 | Move (type sang file/namespace khác) |
| F5 | Copy |
| ⌘⌦ (fn+⌘⌫) | Safe Delete: xoá sau khi kiểm không còn ai dùng |

### 3.5 Build, chạy, debug

| Phím | Lệnh |
|---|---|
| ⌘F9 † | Build Solution (nền tảng gán ⌘F9 cho Build; Rider đặt Build Solution ở đây) |
| ⌃R / ⌃D | Run / Debug cấu hình đang chọn |
| ⌃⇧R / ⌃⇧D | Run / Debug theo ngữ cảnh con trỏ |
| ⌃⌥R / ⌃⌥D | Chọn cấu hình rồi Run / Debug |
| ⌘F2 | Stop |
| ⌘R (trong cửa sổ Run) | Rerun |
| ⌘F8 | Toggle Line Breakpoint |
| ⌥⇧⌘F8 | Breakpoint tạm (tự xoá khi dừng một lần) |
| ⇧⌘F8 | View Breakpoints |
| F8 / F7 / ⇧F7 / ⇧F8 | Step Over / Into / Smart Step Into / Out |
| ⌥⇧F7 | Force Step Into |
| ⌥F9 / ⌥⌘F9 | Run to Cursor / Force Run to Cursor (bỏ qua breakpoint) |
| ⌥⌘R, F9 | Resume |
| ⌥F8 / ⌥⌘F8 | Evaluate Expression / Quick Evaluate |
| ⌥F10 | Show Execution Point |
| ⌥⇧F5 | Attach to Process |

### 3.6 Cửa sổ, tab, giao diện

| Phím | Lệnh | Phím | Lệnh |
|---|---|---|---|
| ⌘1 | Explorer | ⌘2 | Bookmarks |
| ⌘3 | Find | ⌘4 | Run |
| ⌘5 | Debug | ⌘6 | Problems |
| ⌘7 | Structure | ⌘8 | Services (Docker, DB, app đang chạy) |
| ⌘9 | Git | ⌘0 | Commit |
| ⌥F12 | Terminal | Esc | Về editor |
| ⇧⌘F12 | Ẩn/hiện mọi cửa sổ phụ | F12 | Về cửa sổ phụ vừa dùng |
| ⌘W | Đóng tab | ⇧⌘[ / ⇧⌘] | Tab trước / sau |
| ⌘, | Settings | ⌃` | Đổi nhanh theme/keymap/font |
| ⌃⌘F | Toàn màn hình | ⌘↑ | Navigation bar |

### 3.7 Git

| Phím | Lệnh |
|---|---|
| ⌃V | Menu thao tác git (pull, push, branch, stash…) |
| ⌘K † | Commit |
| ⇧⌘K † | Push |
| ⌘T † | Update Project (pull) |
| ⌘9 / ⌘0 | Cửa sổ Git / Commit |

† Không nằm trong file keymap gốc đã đối chiếu (do Rider hoặc module VCS tự khai báo); ghi theo keymap IntelliJ
(macOS) thường dùng. Kiểm trên máy bằng ⇧⌘A gõ "Build Solution", "Commit", "Push".

### 3.8 Unit test

Phím chạy test khác nhau theo keymap và bản Rider. Cách không phụ thuộc phím: bấm **biểu tượng ở lề trái** cạnh tên
class/method test → **Run** hoặc **Debug**; hoặc đặt con trỏ trong test rồi **⌃⇧R / ⌃⇧D** (chạy theo ngữ cảnh).
Cửa sổ **Unit Tests**: **⇧⌘A → "Unit Tests"**. Chi tiết ở [bài 11](11-test-xunit-testcontainers.md).

---

## 4. Thao tác chuột kèm phím

| Thao tác | Kết quả |
|---|---|
| **⌘Click** tên method/class | Go to Declaration |
| **⌥⌘Click** | Go to Implementation |
| **⇧⌘Click** | Go to Type Declaration |
| **⌥⇧Click** | Thêm con trỏ |
| Giữ **⌘** rê chuột lên tên | Xem nhanh kiểu/chữ ký |
| Bấm lề trái cạnh số dòng | Đặt/bỏ breakpoint |

---

## 5. Tự tra phím tắt, không cần bảng này

1. **⇧⌘A**, gõ tên lệnh ("extract method", "reformat"): danh sách hiện phím tắt cạnh mỗi lệnh; ⏎ để chạy luôn.
2. **⌘, → Keymap**: ô tìm theo tên lệnh; biểu tượng kính lúp cạnh ô là **Find Actions by Shortcut** (bấm phím, ra lệnh
   đang gán cho phím đó). Chuột phải một lệnh → **Add Keyboard Shortcut** để tự gán.
3. Plugin **Key Promoter X** ([bài 01 §4.4](01-cai-dat-macos.md)): mỗi lần bạn dùng chuột cho một lệnh có phím tắt,
   nó hiện phím đó và đếm số lần bạn "lẽ ra đã dùng phím".
4. **Help → Keyboard Shortcuts PDF**: bảng in được của đúng keymap đang dùng.

Dùng keymap **Visual Studio (macOS)**: debug là **F10** (Step Over), **F11** (Step Into); phần lớn phím khác cũng
khác. Mọi phím trong bộ bài vẫn tra được bằng cách 1.

---

## 6. Live template và postfix template: gõ ít ra nhiều

**Live template:** gõ tên tắt rồi **Tab**. **⌘J** liệt kê hết; sửa/tạo mới ở **Settings → Editor → Live Templates**.

| Gõ + Tab | Ra |
|---|---|
| `prop` | `public int MyProperty { get; set; }` |
| `ctor` | constructor của class hiện tại |
| `cw` | `Console.WriteLine();` |
| `foreach` | vòng `foreach` |
| `try` | khối `try/catch` |

**Postfix template:** gõ sau một biểu thức, rồi Tab:

| Gõ | Ra |
|---|---|
| `orders.Count.var` | `var count = orders.Count;` |
| `order.notnull` | `if (order != null) { }` |
| `orders.foreach` | `foreach (var order in orders) { }` |
| `GetAsync(id).await` | `await GetAsync(id)` |
| `x > 0.if` | `if (x > 0) { }` |

Danh sách chính xác tuỳ bản Rider: gõ `.` sau biểu thức, các postfix template hiện trong danh sách gợi ý.

---

## 7. Bài luyện ngón tay (15 phút mỗi ngày, trên lab OrderFlow)

Mở `lab/OrderFlow/OrderFlow.sln`. Mỗi bài làm **không chạm chuột**, bấm giờ.

| # | Việc | Phím | Mục tiêu |
|:-:|---|---|---|
| 1 | Mở `OrderCreatedHandler.cs` | ⌘O, gõ `OCH`, ⏎ | < 5 giây |
| 2 | Tới method `HandleAsync`, rồi tới định nghĩa `Order.Process` | ⌘F12 gõ `Handle`; đặt con trỏ lên `Process`, ⌘B | < 10 giây |
| 3 | Ai gọi `Order.Process`? | ⌥F7 | thấy 1 chỗ trong code (`OrderCreatedHandler`) và 4 chỗ trong `OrderTests` |
| 4 | Quay về chỗ cũ | ⌘[ (hai lần) | |
| 5 | Mở `appsettings.json` của Worker | ⇧⌘O gõ `appsettings`, chọn bản trong `OrderFlow.Worker` | |
| 6 | Đổi tên biến `sent` thành `sentCount` trong `OutboxProcessor` | ⇧F6 | Rider đổi cả 5 chỗ, không đụng chữ "sent" trong chuỗi log |
| 7 | Tách khối `if (Total > AutoConfirmLimit) {…} else {…}` trong `Order.Process` thành method | đặt con trỏ trong khối, ⌥↑ tới khi chọn cả if/else, ⌥⌘M | rồi ⌘Z để hoàn tác |
| 8 | Nhân đôi một dòng, đưa lên 2 dòng, xoá đi | ⌘D, ⌥⇧↑ ×2, ⌘⌫ | |
| 9 | Chọn cùng lúc 6 chỗ `HasMaxLength` trong `OrderDbContext.cs` | chọn chữ, ⌃G năm lần (hoặc ⌃⌘G), Esc để thoát | |
| 10 | Chạy test của `OrderTests` | đặt con trỏ trong class, ⌃⇧R | bài 11 |

Tuần đầu chậm hơn dùng chuột là bình thường. Sau khoảng hai tuần, các thao tác 1–6 thành phản xạ và bạn đọc code nhanh hơn hẳn.

---

## 8. Lỗi hay gặp

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| Bấm F8 khi debug thì nhạc phát/dừng, F11 thì lộ màn hình nền | Hàng F đang là phím đa phương tiện | [§1.1](#11-hàng-f1f12-đang-là-phím-âm-lượng-độ-sáng): bật phím F chuẩn, hoặc giữ **fn** |
| ⌃Space đổi bộ gõ thay vì hiện gợi ý code | macOS dùng ⌃Space cho Input Sources | [§1.2](#12-space-đang-dùng-để-đổi-bộ-gõ-trùng-gợi-ý-code) |
| ⇧⌘A bật một cửa sổ Terminal | Dịch vụ *Search man Page Index in Terminal* | [§1.3](#13-a-mở-terminal-tra-man-thay-vì-find-action) |
| ⌃← / ⌃→ nhảy sang màn hình (Space) khác | Phím của Mission Control | Dùng ⇧⌘[ / ⇧⌘], hoặc tắt phím Mission Control ([§1.4](#14----chuyển-màn-hình-spaces-thay-vì-chuyển-tab)) |
| Gõ `dd`, `ow` trong code thành `đ`, `ơ` | Bộ gõ tiếng Việt đang bật | [§1.5](#15-bộ-gõ-tiếng-việt-khi-code) |
| Phím trong bảng làm việc khác hẳn | Rider đang dùng keymap khác (VS Code, Visual Studio, Windows) | **⌘, → Keymap** chọn **IntelliJ (macOS)**, hoặc tra phím của máy bạn bằng ⇧⌘A ([§5](#5-tự-tra-phím-tắt-không-cần-bảng-này)) |

---

## 9. Tự kiểm tra

- [ ] Đã gỡ đủ 4 xung đột ở §1; thử lại: F8 khi debug, ⌃Space, ⇧⌘A, ⇧⌘[.
- [ ] Làm được 15 phím ở §2 không nhìn bảng.
- [ ] Làm bài luyện §7 không chạm chuột; thao tác 1–6 mỗi cái dưới 10 giây.
- [ ] Tra được phím của một lệnh bất kỳ bằng ⇧⌘A hoặc **⌘, → Keymap** (§5).
- [ ] Dùng được ít nhất hai live template hoặc postfix template (§6).

---

## 10. Ranh giới trung thực

- **Nguồn của bảng:** file keymap mã nguồn mở của nền tảng IntelliJ trong repo `JetBrains/intellij-community`
  (`platform/platform-resources/src/keymaps/$default.xml` và `Mac OS X 10.5+.xml`, nhánh `master`, tải ngày
  21/10/2026). Keymap macOS kế thừa `$default`: phím không ghi đè thì lấy từ `$default` và đổi Ctrl ↔ ⌘, đúng quy
  tắc của lớp `MacOSDefaultKeymap`. Đã giải từng lệnh bằng script theo quy tắc đó.
- Theo tài liệu Rider, keymap **IntelliJ (macOS)** của Rider là keymap đó cộng thêm lệnh riêng của Rider. Lệnh riêng
  của Rider và của module git (đánh dấu †) không có trong file đã đối chiếu.
- Live template, postfix template và tên mục trong System Settings ghi theo bản gần đây của Rider và macOS; chưa
  kiểm trên máy thật (môi trường kiểm chứng không có macOS).
- Bạn cập nhật Rider và thấy phím khác bảng: tin **⇧⌘A** trên máy bạn, không tin bảng.
