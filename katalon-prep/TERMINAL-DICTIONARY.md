# Từ điển terminal — luyện code tay 100% trên macOS

> Mục tiêu: **không mở IDE.** Mở terminal, gõ code bằng vim, chạy test, đọc lỗi, sửa, lặp lại.
> Đó đúng là điều kiện của vòng live coding — và là thứ module `00`/`07` tồn tại để bịt.
>
> Mọi lệnh trong file này **đã chạy thử trên máy bạn**. Chỗ nào chưa cài thì ghi rõ là chưa cài.

## 0. Máy bạn hiện có gì (đã kiểm tra)

| Thứ | Trạng thái |
|---|---|
| Shell | **zsh 5.9** (`/bin/zsh`) |
| Editor | **vim 9.1**, **nano** — chưa có `.vimrc` |
| JDK | SDKMAN: `11.0.30-tem`, `21.0.12-tem`, **`25.0.2-tem` (default)** |
| Build Java | Maven **3.9.14** — không có Gradle |
| Python | **uv 0.11.2**, poetry, `python3` → **3.12.13** |
| Có sẵn | `git` `jq` `rg` (ripgrep) `curl` `make` `brew` |
| **Chưa có** | `fzf` `tree` `tmux` `entr` `watch` `bat` `fd` `gh` `jenv` `nvim` `code` |

> Bốn thứ đáng cài nhất, theo thứ tự: `brew install entr fzf tree gh`
> `entr` = tự chạy lại test khi file đổi (§6.3). Không cài vẫn luyện được — §6.2 có cách thay thế.

---

# NHÓM 1 — Điều hướng & sinh tồn trong zsh

| Lệnh | Làm gì |
|---|---|
| `pwd` | Đang ở đâu |
| `cd -` | **Quay lại thư mục trước đó** — dùng liên tục khi nhảy java ↔ python |
| `cd ~-` | Giống `cd -` nhưng dùng được giữa câu lệnh dài |
| `ls -la` | Liệt kê kể cả file ẩn |
| `ls -lt` | Sắp theo **thời gian sửa**, mới nhất trên đầu |
| `find . -name "*.java" -not -path "*/target/*"` | Tìm file, bỏ thư mục build |
| `find . -maxdepth 2 -type d` | Xem cây thư mục 2 tầng (thay `tree` khi chưa cài) |
| `open .` | Mở Finder tại thư mục hiện tại |
| `pbcopy < file` · `pbpaste > file` | **Clipboard của macOS** — copy nội dung file để dán vào form |
| `cmd \| pbcopy` | Copy output lệnh vào clipboard (dán code lên CoderPad) |
| `!!` | Lặp lại lệnh trước |
| `!$` | Tham số cuối của lệnh trước — `vim !$` sau `ls x.java` |
| `Ctrl-R` | **Tìm ngược trong lịch sử lệnh.** Gõ vài chữ, `Ctrl-R` tiếp để lùi |
| `Ctrl-A` / `Ctrl-E` | Về đầu / cuối dòng lệnh |
| `Ctrl-W` | Xoá một từ về trước |
| `Ctrl-U` | Xoá cả dòng đang gõ |
| `Ctrl-C` | Huỷ lệnh đang chạy |
| `Ctrl-Z` rồi `fg` | Tạm dừng / đưa lại foreground |
| `Ctrl-L` | Xoá màn hình |
| `cmd > out.txt 2>&1` | Ghi cả stdout lẫn stderr ra file |
| `cmd \| tee out.txt` | Vừa xem trên màn hình vừa ghi file |
| `cmd \| tail -30` | 30 dòng cuối — dùng suốt với output Maven |
| `cmd \| grep -E "A\|B"` | Lọc nhiều mẫu |
| `time cmd` | Đo thời gian chạy |
| `echo $?` | **Exit code lệnh vừa rồi.** 0 = ok |

**Bẫy zsh so với bash:** zsh nở glob trước khi truyền, nên `-Dtest=Class#method` có `#` bị coi là
chú thích, và `*` bị nở. **Luôn bọc nháy đơn**: `-Dtest='Class$Nested#method'`.

---

# NHÓM 2 — vim, đủ để code tay

Bạn chỉ cần khoảng 30 lệnh. Học 10 lệnh in đậm trước là gõ được ngay hôm nay.

### 2.1 Vào / ra

| Lệnh | Làm gì |
|---|---|
| `vim file.java` | Mở (tạo nếu chưa có) |
| `vim +42 file.java` | Mở **nhảy thẳng tới dòng 42** — dùng khi test báo lỗi ở dòng 42 |
| `vim +/pattern file` | Mở và nhảy tới lần khớp đầu tiên |
| **`:w`** | Lưu |
| **`:wq`** hoặc `ZZ` | Lưu và thoát |
| **`:q!`** | Thoát **không lưu** (khi bạn làm rối) |
| `:e!` | Nạp lại từ đĩa, vứt mọi thay đổi |
| `:w newname.java` | Lưu thành tên khác |

### 2.2 Ba chế độ — hiểu cái này là hiểu vim

```text
NORMAL  ←── Esc ──  INSERT     Esc luôn đưa bạn về NORMAL. Lạc thì bấm Esc.
   │                            NORMAL = ra lệnh, không gõ chữ được
   └── i a o ──→               INSERT = gõ chữ như editor thường
   └── v V Ctrl-v ──→ VISUAL   VISUAL = bôi đen để thao tác
```

### 2.3 Vào chế độ gõ

| Lệnh | Làm gì |
|---|---|
| **`i`** | Chèn **trước** con trỏ |
| `a` | Chèn **sau** con trỏ |
| `A` | Nhảy **cuối dòng** rồi chèn — dùng nhiều nhất khi thêm `;` |
| `I` | Nhảy **đầu dòng** (sau khoảng trắng) rồi chèn |
| **`o`** | Mở **dòng mới bên dưới** rồi chèn |
| `O` | Mở dòng mới **bên trên** |
| `cw` | Xoá từ hiện tại rồi chèn — đổi tên biến nhanh |
| `cc` | Xoá cả dòng rồi chèn |
| `ci"` | Đổi nội dung **trong** cặp nháy kép (con trỏ ở đâu trong đó cũng được) |
| `ci(` `ci{` `ci[` | Đổi nội dung trong ngoặc — cực hợp sửa tham số hàm |

### 2.4 Di chuyển

| Lệnh | Làm gì |
|---|---|
| `h j k l` | trái / xuống / lên / phải |
| **`w`** / `b` | Sang **từ** sau / trước |
| `0` / `$` | Đầu / cuối dòng |
| **`gg`** / **`G`** | Đầu file / cuối file |
| **`42G`** hoặc `:42` | Nhảy tới **dòng 42** |
| `Ctrl-d` / `Ctrl-u` | Xuống / lên nửa màn hình |
| `%` | Nhảy tới ngoặc **khớp** — kiểm tra `{}` có cân không |
| `{` / `}` | Sang đoạn trên / dưới (khối cách nhau bởi dòng trống) |
| `*` | Tìm **từ đang đứng** ở lần xuất hiện tiếp theo |
| **`/chuoi`** rồi `n` / `N` | Tìm xuôi, tiếp / ngược |
| `:noh` | Tắt highlight sau khi tìm |
| **`Ctrl-o`** / `Ctrl-i` | **Quay lại / tiến tới** vị trí vừa nhảy khỏi |

### 2.5 Sửa

| Lệnh | Làm gì |
|---|---|
| **`x`** | Xoá 1 ký tự |
| **`dd`** | Xoá cả dòng (vào clipboard của vim) |
| `3dd` | Xoá 3 dòng |
| `dw` | Xoá 1 từ |
| `D` | Xoá từ con trỏ tới cuối dòng |
| **`yy`** | Copy dòng |
| `3yy` | Copy 3 dòng |
| **`p`** / `P` | Dán sau / trước |
| **`u`** | Undo |
| **`Ctrl-r`** | Redo |
| `.` | **Lặp lại thao tác sửa vừa rồi** — lệnh mạnh nhất của vim |
| `>>` / `<<` | Thụt vào / ra 1 cấp |
| `==` | Tự canh lề dòng hiện tại |
| `gg=G` | **Tự canh lề cả file** — dùng sau khi dán code |
| `J` | Nối dòng dưới vào dòng hiện tại |

### 2.6 Thay thế & nhiều file

| Lệnh | Làm gì |
|---|---|
| `:%s/cu/moi/g` | Thay **toàn file** |
| `:%s/cu/moi/gc` | Thay toàn file, **hỏi từng chỗ** (`y`/`n`/`a`=tất cả) |
| `:s/cu/moi/g` | Chỉ dòng hiện tại |
| `:10,20s/cu/moi/g` | Chỉ dòng 10–20 |
| `:vs file2` | Chia dọc, mở file khác |
| `:sp file2` | Chia ngang |
| `Ctrl-w w` | Nhảy giữa các cửa sổ |
| `Ctrl-w q` | Đóng cửa sổ hiện tại |
| `:!lenh` | **Chạy lệnh shell không thoát vim** — `:!mvn -q -pl 08-system-design test` |
| `:r !lenh` | Chèn output lệnh vào file |
| `Ctrl-z` rồi `fg` | Tạm treo vim ra shell rồi quay lại — nhanh hơn `:!` |

### 2.7 `.vimrc` tối thiểu cho Java + Python

Bạn **chưa có** `.vimrc`. Tạo một lần, dùng mãi:

```bash
cat > ~/.vimrc <<'EOF'
set number relativenumber   " số dòng + số tương đối -> đếm nhanh cho 5dd, 12G
set expandtab tabstop=4 shiftwidth=4 softtabstop=4
autocmd FileType python setlocal tabstop=4 shiftwidth=4
set autoindent smartindent
set incsearch hlsearch ignorecase smartcase
set showmatch               " nháy ngoặc khớp
set colorcolumn=100         " vạch 100 ký tự
set wildmenu                " gợi ý khi gõ :e <Tab>
set backspace=indent,eol,start
set clipboard=unnamed       " yank cua vim = clipboard macOS
syntax on
filetype plugin indent on
" F5 = luu roi chay test cua module dang mo
nnoremap <F5> :w<CR>:!clear && mvn -o -q test<CR>
EOF
```

Kiểm tra: `vim /tmp/thu.java` → thấy số dòng là xong.

---

# NHÓM 3 — Java: đổi JDK, chạy, build

### 3.1 Đổi JDK bằng SDKMAN

`sdk` là **hàm shell**, không phải file thực thi → chỉ chạy trong shell tương tác, và
`sdk use` chỉ ảnh hưởng **shell hiện tại**.

| Lệnh | Làm gì |
|---|---|
| `sdk list java` | Xem mọi bản cài được (dài — `q` để thoát) |
| **`sdk current java`** | Đang dùng bản nào |
| **`sdk use java 21.0.12-tem`** | Đổi **chỉ trong shell này**. Mở tab mới là mất |
| `sdk default java 21.0.12-tem` | Đổi **mặc định toàn máy** (mọi shell mới) |
| `sdk install java 21.0.12-tem` | Cài thêm |
| `sdk env` | **Đọc `.sdkmanrc` trong thư mục hiện tại và áp dụng** ⭐ |
| `sdk env init` | Tạo `.sdkmanrc` với bản đang dùng |
| `sdk env clear` | Trả về mặc định |
| `sdk config` | Mở config — đặt `sdkman_auto_env=true` để **tự đổi khi `cd`** |

**Đã tạo sẵn cho bạn:** `katalon-prep-java/.sdkmanrc` ghim `java=21.0.12-tem`.

```bash
cd katalon-prep-java
sdk env            # -> Using java version 21.0.12-tem
java -version      # xac nhan: openjdk 21.0.12
```

Bật tự động (làm một lần, rất đáng):

```bash
sdk config          # sua sdkman_auto_env=false -> true, luu
# tu do cd vao katalon-prep-java la tu dong sang JDK 21
```

### 3.2 Xem và đặt JAVA_HOME thủ công

| Lệnh | Làm gì |
|---|---|
| `echo $JAVA_HOME` | Đang trỏ đâu |
| `java -version` | Bản đang dùng (in ra **stderr** — cần `2>&1` khi pipe) |
| `javac -version` | Bản compiler |
| `ls ~/.sdkman/candidates/java/` | Các bản đã cài |
| `export JAVA_HOME=~/.sdkman/candidates/java/21.0.12-tem` | Đặt tay cho shell này |
| `JAVA_HOME=~/.sdkman/candidates/java/21.0.12-tem mvn test` | Đặt **chỉ cho một lệnh** ⭐ |

> `/usr/libexec/java_home` **không thấy** JDK của SDKMAN trên máy bạn (đã thử: *"Unable to
> locate a Java Runtime"*). Dùng đường dẫn `~/.sdkman/...` như trên.

### 3.3 Chạy Java một file — không cần Maven

Đây là cách luyện DSA nhanh nhất.

| Lệnh | Làm gì |
|---|---|
| **`java Bai.java`** | **Chạy thẳng file `.java`** (Java 11+), không cần `javac`. Nhanh nhất để luyện |
| `javac Bai.java && java Bai` | Compile ra `.class` rồi chạy — cách cổ điển |
| `javac -d out Bai.java` | Compile vào thư mục `out/` cho gọn |
| `java -cp out Bai` | Chạy với classpath |
| `java -ea Bai` | **Bật `assert`** — mặc định assert bị tắt, rất hay quên |
| `javap -p -c Bai.class` | Xem bytecode (kiểm tra `-parameters` có vào không) |

Vòng luyện DSA gọn nhất:

```bash
cd /tmp && vim TwoSum.java     # gõ tay, có main() tự test
java TwoSum.java               # chạy luôn, khong can javac
```

### 3.4 `jshell` — REPL của Java ⭐

Đã kiểm tra chạy được. Dùng để thử một ý trong 5 giây thay vì tạo cả class.

| Lệnh | Làm gì |
|---|---|
| `jshell` | Mở REPL |
| `jshell -q` | Mở, ít lời chào |
| `jshell file.jsh` | **Chạy file lệnh rồi thoát** — dùng để script |
| `jshell --enable-preview` | Bật tính năng preview |

Trong jshell:

| Lệnh | Làm gì |
|---|---|
| `/help` | Trợ giúp |
| `/list` | Các đoạn đã gõ |
| `/vars` `/methods` `/types` | Liệt kê biến / hàm / kiểu đã định nghĩa |
| `/edit ten` | Sửa một đoạn bằng editor |
| `/save file.jsh` | Lưu phiên ra file |
| `/open file.jsh` | Nạp file vào phiên |
| `/reset` | Xoá sạch làm lại |
| **`/exit`** | Thoát |

```java
jshell> var m = new java.util.HashMap<String,Integer>();
jshell> m.merge("a", 1, Integer::sum)      // thu API ma khong nho chac
jshell> java.util.Set.of("a","a")          // -> nem IllegalArgumentException, thay ngay
```

### 3.5 Maven — bảng tra

Chạy từ `katalon-prep-java/`.

| Lệnh | Làm gì |
|---|---|
| `mvn test` | Build + chạy **toàn bộ** test (9 module) |
| **`mvn -o test`** | **Offline** — không hỏi mạng, nhanh hơn hẳn khi đã có đủ dependency ⭐ |
| `mvn -q test` | Yên lặng — chỉ in lỗi và output của test |
| **`mvn -pl 01-clean-code-solid test`** | **Chỉ một module** ⭐ |
| `mvn -pl 03-spring-boot-compare -am test` | Module đó **+ các module nó phụ thuộc** |
| `mvn clean test` | Xoá `target/` rồi build lại |
| `mvn -o clean test` | Kết hợp — dùng khi đổi `pom.xml` |
| `mvn compile` | Chỉ compile `src/main`, không chạy test |
| `mvn test-compile` | Compile cả test, **không chạy** — kiểm tra cú pháp nhanh |
| `mvn -DskipTests package` | Đóng gói, bỏ qua test |
| `mvn dependency:tree` | Cây phụ thuộc — tìm xung đột version |
| `mvn dependency:tree -Dincludes=org.postgresql` | Lọc theo groupId |
| `mvn help:effective-pom` | POM sau khi gộp hết parent/profile |
| `mvn -X test` | Debug — rất dài, chỉ khi bí |
| `mvn versions:display-dependency-updates` | Có bản mới nào không |

**Chọn test cụ thể** (nhớ **nháy đơn** vì zsh):

| Lệnh | Làm gì |
|---|---|
| `mvn -o -pl 00-refresher-java21 test -Dtest=ResultReporterTest` | Một **class** test |
| `mvn -o -pl 08-system-design test -Dtest='SystemDesignTest$BinPacking'` | Một **`@Nested` class** — `$` ngăn cách |
| `mvn -o -pl 08-system-design test -Dtest='SystemDesignTest$BinPacking#packingIsDeterministic'` | **Một method** |
| `mvn -o -pl 08-system-design test -Dtest='*BinPacking#*Determin*'` | Wildcard cũng được |
| `mvn test -Dsurefire.failIfNoSpecifiedTests=false` | Không lỗi khi pattern không khớp module nào |

> ### ⚠️ BẪY ĐÃ KIỂM CHỨNG — exit code nói dối
>
> | Tình huống | Kết quả |
> |---|---|
> | Class **không tồn tại** | `exit 1`, BUILD FAILURE — an toàn |
> | Class có, **method gõ sai** | **`Tests run: 0`** nhưng **`exit 0`, BUILD SUCCESS** ⚠️ |
> | `@Nested` mà viết `Outer#method` (thiếu `$Nested`) | **`Tests run: 0`**, vẫn SUCCESS ⚠️ |
>
> **Luôn đọc dòng `Tests run: N`, đừng tin mỗi màu xanh.** Đây là cách dễ nhất để tưởng
> mình vừa chạy test mà thật ra không chạy gì cả.

**Đọc kết quả nhanh:**

```bash
mvn -o test 2>&1 | grep -E "Tests run:|BUILD|ERROR" | tail -20
mvn -o test 2>&1 | tee /tmp/mvn.log | tail -30    # giu log de doc ky
grep -A30 "<<< FAILURE" /tmp/mvn.log              # xem chi tiet cho fail
cat 01-clean-code-solid/target/surefire-reports/*.txt   # bao cao day du
```

### 3.6 Chạy Quarkus / Spring Boot

| Lệnh | Làm gì |
|---|---|
| `mvn -pl 02-quarkus-service quarkus:dev` | **Live reload** — sửa file, F5 trình duyệt là thấy |
| `mvn -pl 03-spring-boot-compare spring-boot:run` | Chạy service Spring |
| `curl -s localhost:8080/q/health \| jq` | Gọi thử endpoint, format JSON |
| `curl -s -X POST localhost:8080/api/runs -H 'Content-Type: application/json' -d '{"tenantId":"t1"}' \| jq` | POST có body |
| `lsof -iTCP:8080 -sTCP:LISTEN` | **Ai đang chiếm cổng 8080** |
| `kill -9 $(lsof -tiTCP:8080)` | Giết tiến trình đang chiếm cổng |

---

# NHÓM 4 — Python: uv, venv, pytest

Chạy từ `katalon-prep-python/`.

### 4.1 Môi trường

| Lệnh | Làm gì |
|---|---|
| **`uv sync`** | Tạo `.venv` + cài đúng theo `uv.lock` ⭐ |
| `uv sync --extra llm` | Cài thêm nhóm tuỳ chọn (gọi Claude thật) |
| `uv sync --extra spark` | Cài PySpark (**368 MB**) |
| **`uv run <lệnh>`** | Chạy trong venv **mà không cần activate** ⭐ |
| `source .venv/bin/activate` | Activate kiểu cũ (dấu nhắc có `(katalon-prep-python)`) |
| `deactivate` | Thoát venv |
| `uv add requests` | Thêm dependency vào `pyproject.toml` + cài |
| `uv add --dev pytest-cov` | Thêm vào nhóm dev |
| `uv remove requests` | Gỡ |
| `uv lock` | Cập nhật `uv.lock` |
| `uv pip list` | Xem đã cài gì |
| `uv python list` | Các bản Python uv thấy được |
| `uv venv --python 3.12` | Tạo venv với bản cụ thể |
| `rm -rf .venv` | **Xoá sạch môi trường.** Không đụng gì tới hệ thống |
| `which python3` | Đang dùng python nào |

> **Bẫy PATH trên máy bạn:** `python3` → **3.12.13** (`/opt/homebrew/opt/python@3.12/libexec/bin`),
> nhưng `/opt/homebrew/bin/python3` là **3.14.3**. Còn `uv` tự dùng **3.13.5** trong `.venv`.
> **Ba bản khác nhau.** Vì vậy luôn dùng `uv run`, đừng gọi `python3` trực tiếp trong project này.

### 4.2 pytest — bảng tra

| Lệnh | Làm gì |
|---|---|
| **`uv run pytest -q`** | Chạy toàn bộ, gọn ⭐ |
| `uv run pytest -v` | Hiện tên từng test |
| **`uv run pytest -x`** | **Dừng ngay ở lỗi đầu tiên** ⭐ |
| `uv run pytest --lf` | **Chỉ chạy lại các test vừa FAIL** (last-failed) ⭐ |
| `uv run pytest --ff` | Chạy test fail trước, rồi mới tới phần còn lại |
| `uv run pytest tests/test_09_agent.py` | Một file |
| `uv run pytest tests/test_09_agent.py::test_trace_doc_duoc_bang_mat` | **Một test** |
| `uv run pytest tests/test_07_dsa.py::TestTwoSum` | Một class |
| **`uv run pytest -k "jitter or breaker"`** | Lọc theo **tên** — không cần nhớ file ⭐ |
| `uv run pytest -k "not slow"` | Loại trừ |
| `uv run pytest -m spark` | Lọc theo **marker** |
| `uv run pytest -m "not slow"` | Bỏ test chậm |
| **`uv run pytest -s`** | **Không nuốt `print`** — thấy số đo in ra ⭐ |
| `uv run pytest -q -s` | Kết hợp: gọn + thấy print |
| `uv run pytest --tb=short` | Traceback ngắn |
| `uv run pytest --tb=line` | Mỗi lỗi 1 dòng |
| `uv run pytest --tb=no` | Không traceback, chỉ tên |
| `uv run pytest --collect-only -q` | **Liệt kê test mà không chạy** |
| `uv run pytest --collect-only -q \| grep -c ::` | Đếm số test |
| `uv run pytest --durations=10` | 10 test chậm nhất |
| `uv run pytest -p no:randomly` | Tắt xáo trộn thứ tự (nếu có plugin) |
| **`uv run pytest --pdb`** | **Rơi vào debugger ngay tại chỗ fail** ⭐ |
| `uv run pytest -q 2>&1 \| tail -20` | Chỉ xem phần cuối |

### 4.3 Lint & format

| Lệnh | Làm gì |
|---|---|
| **`uv run ruff check src tests`** | Bắt lỗi (đã cấu hình như SonarQube) |
| `uv run ruff check --fix src tests` | **Tự sửa** những gì sửa được |
| `uv run ruff check --output-format=concise src` | Mỗi lỗi 1 dòng, dễ đọc |
| `uv run ruff format src tests` | Format code |
| `uv run ruff check --statistics src` | Thống kê theo rule |

### 4.4 Chạy Python thủ công

| Lệnh | Làm gì |
|---|---|
| `uv run python bai.py` | Chạy file |
| **`uv run python -i bai.py`** | Chạy xong **ở lại REPL** với mọi biến còn nguyên ⭐ |
| `uv run python -m prep.agent.demo` | Chạy module (dùng `__main__`) |
| `uv run python -c "print(-7 % 3)"` | Chạy một dòng |
| **`uv run python`** | REPL — đối ứng `jshell` bên Java |
| `uv run python -m pdb bai.py` | Chạy dưới debugger từ đầu |
| `uv run python -X dev bai.py` | Bật cảnh báo phát triển (bắt lỗi ẩn) |
| `uv run python -m json.tool < f.json` | Format JSON |
| `uv run python -m http.server 8000` | Web server tĩnh tại chỗ |

> **Bẫy `multiprocessing` trên macOS đã gặp thật:** `ProcessPoolExecutor` **không chạy được**
> khi code đến từ stdin (`python - <<EOF`) — macOS dùng `spawn`, process con cần đọc lại file
> nguồn. Lỗi: `FileNotFoundError: '<stdin>'`. **Phải ghi ra file `.py` rồi chạy**, và đặt phần
> chạy trong `if __name__ == "__main__":`.

---

# NHÓM 5 — Debug từ terminal

### 5.1 Python — `pdb`

Chèn vào code: `breakpoint()` (Python 3.7+), rồi chạy bình thường.

| Lệnh trong pdb | Làm gì |
|---|---|
| `l` | Xem code quanh chỗ đang dừng |
| `ll` | Xem cả hàm |
| `n` | Chạy dòng tiếp (**không** vào trong hàm) |
| `s` | Bước **vào** trong hàm |
| `c` | Chạy tiếp tới breakpoint sau |
| `r` | Chạy tới khi hàm hiện tại return |
| `p bien` | In giá trị |
| `pp bien` | In đẹp (dict/list lớn) |
| `w` | Stack trace — đang ở đâu trong chuỗi gọi |
| `u` / `d` | Lên / xuống một khung stack |
| `b 42` | Đặt breakpoint ở dòng 42 |
| `b module:hàm` | Breakpoint theo tên |
| `q` | Thoát |
| `interact` | Mở REPL đầy đủ tại khung hiện tại |

`uv run pytest --pdb` = tự rơi vào pdb đúng chỗ fail — **nhanh hơn rải `print` rất nhiều**.

### 5.2 Java

| Lệnh | Làm gì |
|---|---|
| `jps -l` | Liệt kê tiến trình JVM đang chạy |
| `jstack <pid>` | **Dump toàn bộ thread** — tìm deadlock, thread bị treo |
| `jstack <pid> \| grep -A5 "deadlock"` | Lọc phần deadlock |
| `jmap -histo <pid> \| head -20` | 20 loại object chiếm bộ nhớ nhất |
| `jcmd <pid> VM.flags` | Các flag JVM đang chạy |
| `jcmd <pid> GC.heap_info` | Thông tin heap |
| `jdb -attach <port>` | Debugger dòng lệnh (hiếm dùng) |
| `mvn -pl X test -Dmaven.surefire.debug` | **Treo test chờ debugger** ở cổng 5005 |

Trong code, cách nhanh nhất vẫn là: `System.out.println()` tạm, hoặc **viết một test nhỏ**.
Module `00`/`01` được thiết kế đúng để bạn làm thế.

---

# NHÓM 6 — Vòng lặp luyện tập

### 6.1 Vòng chuẩn (một cửa sổ terminal)

```bash
# JAVA
cd katalon-prep-java && sdk env
vim 07-dsa-drill/src/main/java/com/prep/drill/DrillWorkspace.java
mvn -o -pl 07-dsa-drill test -Dtest='DrillWorkspaceTest$TwoSum'
#  ↑ mũi tên lên để lặp lại, hoặc Ctrl-R gõ "TwoSum"

# PYTHON
cd katalon-prep-python
vim src/prep/dsa/workspace.py
uv run pytest tests/test_07_dsa.py::TestTwoSum -q
```

### 6.2 Không thoát vim (chưa cần cài gì)

```vim
:!mvn -o -q -pl 07-dsa-drill test -Dtest='DrillWorkspaceTest$TwoSum'
:!uv run pytest tests/test_07_dsa.py -q
:!!                     " lap lai lenh shell truoc do
```
Hoặc `Ctrl-z` ra shell → chạy → `fg` quay lại vim. **Nhanh hơn `:!`** vì vim không vẽ lại màn hình.

### 6.3 Tự chạy lại khi file đổi (cần cài `entr`)

```bash
brew install entr

# Java: doi file .java nao trong module la chay lai test
find 07-dsa-drill/src -name '*.java' | entr -c mvn -o -q -pl 07-dsa-drill test

# Python
find src tests -name '*.py' | entr -c uv run pytest -q -x
```
`-c` = xoá màn hình trước mỗi lần chạy. Mở 2 cửa sổ: một cái vim, một cái `entr`.

### 6.4 Không có `entr` — vòng lặp thuần zsh

```bash
while true; do clear; uv run pytest -q -x; echo "\n--- Enter de chay lai, Ctrl-C de thoat ---"; read; done
```

### 6.5 Luyện DSA có bấm giờ

```bash
cd /tmp && vim TwoSum.java
time java TwoSum.java          # do thoi gian CHAY

# do thoi gian LAM BAI: mo cua so khac
date +%s > /tmp/start && vim TwoSum.java && echo "$(( $(date +%s) - $(cat /tmp/start) )) giay"
```

> **Tắt Copilot trước khi luyện.** Trong VSCode: `Cmd-Shift-P` → *Disable Copilot Completions*.
> Trong vim thì mặc định đã không có — đó là **ưu điểm** của việc luyện bằng vim.

---

# NHÓM 7 — Tìm kiếm trong code

`rg` (ripgrep) đã có sẵn, nhanh hơn `grep` nhiều và tự bỏ qua `target/`, `.venv/`.

| Lệnh | Làm gì |
|---|---|
| `rg "HealthCheck"` | Tìm chuỗi trong cả cây thư mục |
| `rg -i "healthcheck"` | Không phân biệt hoa thường |
| `rg -w "run"` | Khớp **cả từ** — tránh khớp `runner`, `prerun` |
| `rg -n "TODO"` | Kèm số dòng (mặc định đã có) |
| `rg -l "CircuitBreaker"` | **Chỉ liệt kê tên file** |
| `rg -c "assert"` | Đếm số khớp mỗi file |
| `rg -t java "record "` | Chỉ file Java |
| `rg -t py "async def"` | Chỉ file Python |
| `rg -A3 -B3 "grounding_score"` | Kèm 3 dòng trước/sau |
| `rg --files -g '*.java'` | Liệt kê mọi file Java |
| `rg "def test_" -c tests/` | Đếm test mỗi file |
| `rg -F 'a[0]'` | Tìm **nguyên văn**, không coi là regex |
| `grep -rn "chuoi" .` | Cách cũ khi không có `rg` |

Nhảy thẳng tới chỗ tìm được:

```bash
rg -n "grounding_score" src/          # thay: src/prep/agent/testgen.py:112
vim +112 src/prep/agent/testgen.py
```

---

# NHÓM 8 — Git

Thư mục này **hiện không phải git repo**. Nếu muốn theo dõi tiến độ luyện tập:

| Lệnh | Làm gì |
|---|---|
| `git init` | Khởi tạo repo |
| `git status -sb` | Trạng thái gọn |
| `git add -p` | **Thêm từng đoạn một** — buộc bạn đọc lại diff của chính mình ⭐ |
| `git diff` | Xem thay đổi chưa stage |
| `git diff --staged` | Xem thay đổi đã stage |
| `git diff --stat` | Chỉ thống kê số dòng |
| `git commit -m "..."` | Commit |
| `git log --oneline -10` | 10 commit gần nhất |
| `git log --oneline --graph --all` | Cây nhánh |
| `git stash` / `git stash pop` | Cất tạm / lấy lại |
| `git checkout -- file` | **Vứt thay đổi** của một file |
| `git restore file` | Như trên, cú pháp mới |
| `git switch -c nhanh-moi` | Tạo và sang nhánh mới |
| `git blame file` | Ai sửa dòng nào |

> `git add -p` là công cụ luyện tập tốt: nó bắt bạn nhìn lại từng đoạn thay đổi trước khi commit
> — đúng thói quen mà mục "review từng dòng output của AI" cần.

---

# NHÓM 9 — Bẫy macOS / zsh đã gặp thật

| Bẫy | Triệu chứng | Cách xử lý |
|---|---|---|
| **zsh nở glob** | `-Dtest=A#b` mất phần sau `#`; `*` bị nở thành tên file | **Luôn nháy đơn**: `-Dtest='A$B#c'` |
| **`java -version` ra stderr** | `java -version \| grep 21` không khớp | `java -version 2>&1 \| grep 21` |
| **`sdk` là hàm shell** | `sdk use` trong script không chạy | Dùng `zsh -ic 'sdk use ...'`, hoặc set `JAVA_HOME` trực tiếp |
| **`sdk use` chỉ trong shell đó** | Tab mới lại về JDK 25 | `.sdkmanrc` + `sdk env`, hoặc `sdk default` |
| **`timeout` không có sẵn** | `command not found: timeout` | `brew install coreutils` → dùng `gtimeout` |
| **`/usr/libexec/java_home` không thấy SDKMAN** | *"Unable to locate a Java Runtime"* | Dùng đường dẫn `~/.sdkman/candidates/java/...` |
| **Ba bản Python cùng lúc** | Cài thư viện xong `import` vẫn lỗi | Luôn `uv run`, đừng gọi `python3` trực tiếp |
| **`multiprocessing` từ stdin** | `FileNotFoundError: '<stdin>'` | Ghi ra file `.py` + `if __name__ == "__main__":` |
| **Maven không recompile khi chỉ đổi pom** | Sửa `pom.xml` mà không có tác dụng | `mvn clean test` |
| **`Tests run: 0` vẫn BUILD SUCCESS** | Tưởng test pass, thật ra không chạy gì | **Đọc dòng `Tests run:`**, đừng tin exit code |
| **Spark vỡ trên JDK 25** | `getSubject is not supported` | `export JAVA_HOME=~/.sdkman/candidates/java/21.0.12-tem` |
| **`.DS_Store` lẫn vào** | Rác trong `git status` | Thêm vào `.gitignore` |

---

# NHÓM 10 — Alias nên thêm vào `~/.zshrc`

```bash
cat >> ~/.zshrc <<'EOF'

# ---------- katalon-prep ----------
export KP="$HOME/data/projects/success/motives/motivesidp-ai-learning/motives-algorithm/katalon-prep"
alias kp='cd $KP'
alias kpj='cd $KP/katalon-prep-java && sdk env'
alias kpp='cd $KP/katalon-prep-python'

# Java
alias mt='mvn -o -q test'                     # test toan bo, offline, gon
alias mtc='mvn -o clean test'                 # sach roi test
alias mpl='mvn -o -q -pl'                     # vd: mpl 01-clean-code-solid test
alias mres='cat target/surefire-reports/*.txt'
alias j21='export JAVA_HOME=~/.sdkman/candidates/java/21.0.12-tem && java -version'

# Python
alias pt='uv run pytest -q'
alias ptx='uv run pytest -q -x'               # dung o loi dau tien
alias ptl='uv run pytest -q --lf'             # chi chay lai cai vua fail
alias pts='uv run pytest -q -s'               # thay print
alias rk='uv run ruff check src tests'
alias rkf='uv run ruff check --fix src tests'

# Tien ich
alias ll='ls -lah'
alias ports='lsof -iTCP -sTCP:LISTEN -P -n'
EOF

source ~/.zshrc     # nap lai ngay, khong can mo tab moi
```

---

# PHỤ LỤC A — Nhảy qua lại hai môi trường

```bash
# ------- sang JAVA -------
kpj                 # cd + sdk env  -> JDK 21 tu dong
java -version       # xac nhan: 21.0.12
mt                  # 191 test

# ------- sang PYTHON -------
kpp                 # cd
uv run python -V    # xac nhan ban trong .venv
pt                  # 122 test

# ------- ve JDK mac dinh -------
sdk env clear       # hoac mo tab moi
```

Hai môi trường **không đụng nhau**: SDKMAN chỉ đổi `JAVA_HOME`/`PATH` cho Java, `uv` chỉ dùng
`.venv` trong thư mục Python. Nhảy qua lại tuỳ ý.

# PHỤ LỤC B — 20 lệnh dùng 90% thời gian

```text
VIM        i  Esc  :w  :wq  :q!  dd  yy  p  u  /tim  n  gg  G  42G  o  A  ci"  .  gg=G
NAV        cd -   Ctrl-R   !$   Ctrl-A/E   ls -lt
JAVA       sdk env · java Bai.java · jshell · mvn -o -q -pl X test -Dtest='A$B#c'
PYTHON     uv sync · uv run pytest -q -x · --lf · -k "ten" · -s · --pdb · uv run ruff check --fix
TIM        rg -n "chuoi"  →  vim +<dong> <file>
DOC LOI    ... 2>&1 | tail -30    ·    grep -E "Tests run:|FAIL"
```

# PHỤ LỤC C — Kế hoạch 5 ngày làm quen terminal

| Ngày | Việc | Xong là được |
|---|---|---|
| 1 | Tạo `~/.vimrc` (§2.7) + alias (§10). Mở `DrillWorkspace.java` bằng vim, chỉ dùng `i Esc :w :q` | Gõ và lưu được, không hoảng |
| 2 | Giải `two_sum` **bằng vim, tắt Copilot**, chạy bằng `java TwoSum.java` | Không đụng chuột |
| 3 | Thêm `dd yy p u /tim 42G .` vào tay. Giải `level_order` cả Java lẫn Python | Sửa nhanh không cần mũi tên |
| 4 | Luyện chọn 1 test: `mvn -Dtest='A$B#c'` và `pytest -k`. **Cố tình gõ sai tên method** để thấy bẫy `Tests run: 0` | Không bao giờ tin mỗi màu xanh nữa |
| 5 | `brew install entr`, dựng vòng tự chạy lại (§6.3). Giải `can_finish` có bấm giờ | Vòng edit→test dưới 3 giây |

Sau 5 ngày, mọi buổi luyện `07-dsa-drill` đều làm trong terminal. **Đó chính là điều kiện của
vòng live coding** — khác biệt duy nhất là lúc đó có người ngồi xem.
