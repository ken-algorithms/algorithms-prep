#!/usr/bin/env python3
"""Build the single-page "Algorithms Learning" app from the markdown in this repo.

    uv run --with markdown --with pymdown-extensions python3 web/build.py

Writes two files:
  web/algorithms-learning.html  → for publishing as a Claude Artifact (no <!doctype>/<html>/<head>/<body>)
  web/index.html                → standalone, open directly in a browser

Everything is embedded; the page runs fully offline. Re-run after editing any .md file.
"""
from __future__ import annotations

import json
import os
import posixpath
import re
from pathlib import Path

import markdown
import markdown.extensions.toc as md_toc


def gh_unique(id: str, ids: set) -> str:
    """Heading trùng tên: GitHub thêm -1, -2… (python-markdown mặc định thêm _1). Làm giống GitHub để anchor
    viết trong file .md (ví dụ #đọc-1 của tuần 7) mở được cả trên GitHub lẫn ở bản dựng này."""
    base, n = id, 0
    while id in ids or not id:
        n += 1
        id = f"{base}-{n}"
    ids.add(id)
    return id


md_toc.unique = gh_unique  # toc gọi unique() qua biến toàn cục của module, nên thay ở đây là đủ

ROOT = Path(__file__).resolve().parent.parent
WEB = ROOT / "web"
VIDEO_ROOT = ROOT / "leetcode-38-bai-video"
# Bản mở trực tiếp (web/index.html) trỏ thẳng vào thư mục video; bản Artifact cần publish kèm
# các file media dưới đường dẫn "media/<đường dẫn tương đối trong leetcode-38-bai-video>".
# GitHub Pages deploy web/index.html lên gốc site nên đặt MEDIA_BASE=media/ và copy media vào _deploy/media/
# (xem .github/workflows/pages.yml).
MEDIA_BASE_STANDALONE = os.environ.get("MEDIA_BASE", "../leetcode-38-bai-video/")
MEDIA_BASE_ARTIFACT = "media/"
# Video bài giảng Java System Design (tools/lesson_video): mỗi bộ một thư mục java-system-design/<id>/,
# trong đó lessons/ có mp4 + jpg + lessons.json. Bản mở trực tiếp trỏ thẳng vào lessons/; GitHub Pages
# và bản Artifact dùng media/jsd/<id>/ (workflow copy sang).
JSD_ROOT = ROOT / "java-system-design"
JSD_SERIES = [
    {"id": "video-gd1", "title": "Giai đoạn 1 · Nền tảng (tuần 1–4)", "voice": "Tom · Kokoro am_michael",
     "total": 30, "coverage": "02-do-phu-tuan-1.md"},
    {"id": "video-gd2", "title": "Giai đoạn 2 · Dữ liệu và hệ phân tán (tuần 5–10)", "voice": "Emma · Kokoro af_heart",
     "total": 23, "coverage": "02-do-phu.md", "weeks": {"5": "Tuần 5–6", "8": "Tuần 8–9"}},  # khối hai tuần
    # bộ chen ngang: mã video K00…; "tuần" là mốc nên xem (sau tuần 4 = hết giai đoạn 1, sau tuần 10 = hết giai đoạn 2);
    # nguồn nằm ở katalon-prep/ nên covers có dạng ../katalon-prep/… (chuẩn hoá đường dẫn trước khi tra tài liệu)
    {"id": "video-katalon", "title": "Chen ngang Katalon · họ A + C: 10k → 10M → 100M request/phút",
     "voice": "Tom hỏi (am_michael), Emma trả lời (af_heart)", "total": 14, "coverage": "02-do-phu.md", "prefix": "K",
     "weeks": {"4": "Phần 1A: Sau giai đoạn 1 (Ước lượng & 10k)",
               "10": "Phần 1B: Sau giai đoạn 2 (Kiến trúc 10M–100M)",
               "11": "Phần 2: Thực chiến Code & Sửa lỗi Kafka (K08–K13)"}},
]


def jsd_base(sid: str, standalone: bool) -> str:
    if not standalone:
        return MEDIA_BASE_ARTIFACT + f"jsd/{sid}/"
    return (os.environ["MEDIA_BASE"] + f"jsd/{sid}/") if os.environ.get("MEDIA_BASE") \
        else f"../java-system-design/{sid}/lessons/"
SKIP = {".git", "site", "web", "__pycache__", "target", ".venv", "node_modules"}


def gh_slug(text: str, separator: str = "-") -> str:
    """GitHub's anchor algorithm — the .md files contain hand-written anchors that rely on it."""
    text = re.sub(r"`", "", text.strip())
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"<[^>]+>", "", text).lower()
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    return re.sub(r"\s", separator, text)


def first_heading(md_text: str) -> str | None:
    fence = False
    for line in md_text.splitlines():
        if line.startswith("```"):
            fence = not fence
            continue
        if fence:
            continue
        m = re.match(r"^#\s+(.*)$", line)
        if m:
            t = re.sub(r"`", "", m.group(1)).strip()
            return re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", t)
    return None


LINK_RE = re.compile(r'(?<=\]\()([^)\s]+?)(\)|\s)')


def to_doc_refs(md_text: str, page_dir: Path, known: set[str]) -> str:
    """Turn cross-document .md links into in-app references the page can intercept."""
    def sub(m: re.Match) -> str:
        target, tail = m.group(1), m.group(2)
        if target.startswith(("http://", "https://", "mailto:", "#")):
            return target + tail
        path, _, anchor = target.partition("#")
        try:
            rel = (page_dir / path.rstrip("/")).resolve().relative_to(ROOT).as_posix()
        except ValueError:
            return target + tail
        if path.endswith(".md"):
            return f'#doc:{rel}:{anchor}' + tail if rel in known else target + tail
        # link tới thư mục: trỏ vào README của nó nếu có
        if rel + "/README.md" in known:
            return f'#doc:{rel}/README.md:{anchor}' + tail
        return target + tail
    return LINK_RE.sub(sub, md_text)


def collect_docs() -> dict[str, dict]:
    paths = []
    for p in sorted(ROOT.rglob("*.md")):
        # thư mục ẩn (.pytest_cache, .claude…) không phải tài liệu, kể cả khi chỉ có ở máy đang build
        if any(part in SKIP or part.startswith(".") for part in p.relative_to(ROOT).parts):
            continue
        paths.append(p)
    known = {p.relative_to(ROOT).as_posix() for p in paths}

    md = markdown.Markdown(extensions=[
        "tables", "fenced_code", "toc", "attr_list", "sane_lists",
        "md_in_html", "footnotes", "pymdownx.superfences",
    ], extension_configs={"toc": {"slugify": gh_slug, "permalink": "¶", "toc_depth": "2-3"}})

    # Put the two entry points first so the sidebar reads in a sensible order.
    def sort_key(p: Path) -> tuple:
        rel = p.relative_to(ROOT).as_posix()
        head = (
            0 if rel == "README.md" else
            1 if rel.startswith("java-system-design/") else
            2 if rel.startswith("ai-agent-learning/") else
            3 if rel.startswith("ai-learning/") else
            4 if rel.startswith("nab-prep/") else 5
        )
        return (head, rel)

    docs: dict[str, dict] = {}
    for p in sorted(paths, key=sort_key):
        rel = p.relative_to(ROOT).as_posix()
        raw = p.read_text(encoding="utf-8", errors="ignore")
        md.reset()
        html_body = md.convert(to_doc_refs(raw, p.parent, known))
        # rewrite the placeholder hrefs into data attributes the app listens on
        html_body = re.sub(
            r'href="#doc:([^:"]+):([^"]*)"',
            lambda m: f'href="#" data-doc="{m.group(1)}" data-anchor="{m.group(2)}"',
            html_body)
        plain = re.sub(r"<[^>]+>", " ", html_body)
        plain = re.sub(r"\s+", " ", plain).strip()
        docs[rel] = {"t": first_heading(raw) or p.stem, "h": html_body, "x": plain[:4000]}
    return docs


def collect_algos() -> list[dict]:
    """Parse the 38-problem index table out of the DSA document — one source of truth."""
    src = (ROOT / "leetcode-38-bai-phong-van-vietnam.md").read_text(encoding="utf-8")
    rows = []
    pat = re.compile(
        r"^\|\s*([ABC])\s*\|\s*(\d+)\s*\|\s*\[([^\]]+)\]\([^)]*\)\s*\|\s*#(\d+)\s*\|"
        r"\s*(Easy|Medium|Hard)\s*\|\s*([^|]+?)\s*\|", re.M)
    for m in pat.finditer(src):
        rows.append({"g": m.group(1), "i": int(m.group(2)), "n": m.group(3),
                     "lc": int(m.group(4)), "d": m.group(5), "p": m.group(6).strip()})
    media = collect_media()
    for row in rows:
        if row["i"] in media:
            row["v"] = media[row["i"]]
    return rows


def collect_media() -> dict[int, dict]:
    """leetcode-38-bai-video/nhom-*/bai-<N>-*/bai-<N>-full.mp4 và bai-<N>.gif → {N: {mp4, gif, mb}}."""
    media = {}
    for folder in sorted(VIDEO_ROOT.glob("nhom-*/bai-*")):
        m = re.match(r"bai-(\d+)-", folder.name)
        if not m:
            continue
        i = int(m.group(1))
        entry = {}
        for kind, name in (("mp4", f"bai-{i}-full.mp4"), ("gif", f"bai-{i}.gif")):
            f = folder / name
            if f.exists():
                entry[kind] = f.relative_to(VIDEO_ROOT).as_posix()
                entry[kind + "_mb"] = round(f.stat().st_size / 1_048_576, 1)
        if entry:
            media[i] = entry
    return media


def collect_jsd_videos(sid: str) -> list[dict]:
    """Mục lục một bộ video từ lessons.json (do tools/lesson_video ghi); bỏ mục nào thiếu file mp4."""
    vdir = JSD_ROOT / sid / "lessons"
    path = vdir / "lessons.json"
    if not path.exists():
        return []
    out = []
    for x in json.loads(path.read_text(encoding="utf-8")).get("lessons", []):
        mp4 = vdir / x["file"]
        if not mp4.exists():
            print(f"⚠ {sid}/lessons.json có {x['file']} nhưng không thấy file — bỏ qua")
            continue
        x = dict(x, mb=round(mp4.stat().st_size / 1e6, 1))  # MB = 10⁶ byte, như công cụ dựng và bảng trạng thái
        if x.get("poster") and not (vdir / x["poster"]).exists():
            x["poster"] = ""
        out.append(x)
    return sorted(out, key=lambda v: v["ep"])


def collect_jsd_series(docs: dict) -> list[dict]:
    """Các bộ video đã có ít nhất một video. Mã video phải khác nhau giữa các bộ (trạng thái "đã xem" lưu theo mã)."""
    out, seen = [], {}
    for meta in JSD_SERIES:
        lessons = collect_jsd_videos(meta["id"])
        if not lessons:
            continue
        for v in lessons:
            if v["id"] in seen:
                raise SystemExit(f"mã video {v['id']} trùng giữa {seen[v['id']]} và {meta['id']}")
            seen[v["id"]] = meta["id"]
            for ref in v.get("covers", []) + [p["src"] for p in v.get("points", []) if p.get("src")]:
                f, _, anchor = ref.partition("#")      # mỗi anchor nguồn (covers, points) phải mở được trong app
                key = posixpath.normpath(SDR_DIR + f)  # ../katalon-prep/… của bộ chen ngang
                if key not in docs or (anchor and f'id="{anchor}"' not in docs[key]["h"]):
                    print(f"⚠ video {v['id']}: không mở được {ref}")
        d = SDR_DIR + meta["id"] + "/"
        out.append(dict(meta, lessons=lessons, plan=d + "00-ke-hoach-va-lich-su.md",
                        glossary=d + "01-bang-chu-viet-tat.md", coverage=d + meta["coverage"]))
    return out


# ---- hand-authored, derived from nab-prep/ and katalon-prep/ --------------------------------
COMPANIES = {
    "nab": {
        "name": "NAB Innovation Centre",
        "role": "Senior / Lead Java Engineer · HCM + Hà Nội",
        "blurb": "Product company của ngân hàng lớn nhất Úc. Không phải outsourcing — domain nghiệp "
                 "vụ ngân hàng được coi trọng, và đó đúng là thế mạnh lớn nhất của bạn.",
        "stack": ["Java 8+", "Spring Boot", "AWS", "Kubernetes", "Terraform", "Kafka (preferred)",
                  "Jenkins", "Docker", "Golang"],
        "note": "Hai biến thể quy trình được báo cáo (5 vòng trên voz, 3 vòng trong một bài kể chi "
                "tiết). Đây là bản hợp nhất phần chắc chắn xuất hiện ở mọi nguồn.",
        "rounds": [
            {"t": "Sàng lọc tự động", "d": "Trắc nghiệm Java/Spring online, hoặc 3 bài thuật toán"},
            {"t": "HR", "d": "Giới thiệu, điểm mạnh/yếu, mục tiêu 2 năm", "eng": True},
            {"t": "Live coding", "d": "LeetCode — một nguồn nói hard, nguồn khác nói medium. Có thể trên Codility"},
            {"t": "Technical với Tech Lead", "d": "Java core, Spring (Beans/IoC/Security/JPA), dự án, git, sort/search, Docker"},
            {"t": "Engineering Manager", "d": "System design sâu: microservices, Saga, event-driven, commit log + ~20% behavioral", "eng": True},
        ],
        "req": [
            {"t": "Banking / financial services", "lvl": "ok", "why": "Sổ cái kế toán kép thật — phần lớn ứng viên Java không có",
             "doc": "katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md"},
            {"t": "Cloud — AWS / Azure", "lvl": "ok", "why": "ECS, ALB, StepScaling, Aurora, Lambda — Terraform thật có file:line",
             "doc": "katalon-prep/katalon-system-design/05-he-thong-that-allinone-aws.md"},
            {"t": "Microservices + RESTful API", "lvl": "ok", "why": "6+ service qua Spring Cloud Gateway, phân tích được cả điểm yếu"},
            {"t": "Containers — ECS, Docker", "lvl": "ok", "why": "ECS Fargate production thật"},
            {"t": "CI/CD — Jenkins, Git, Gradle", "lvl": "ok", "why": "CodeBuild, Jenkins trên ECS Fargate, S3 + DynamoDB state locking"},
            {"t": "AWS Lambda / FaaS", "lvl": "warn", "why": "Có thật nhưng ở tầng vận hành, không phải business logic"},
            {"t": "Java 8+, Spring Boot", "lvl": "warn", "why": "Nền vững nhưng ~1 năm không viết hằng ngày — cần làm nóng tay"},
            {"t": "Unit / integration test", "lvl": "warn", "why": "Cần dẫn chứng cụ thể hơn ở dự án thật"},
            {"t": "Kubernetes", "lvl": "warn", "why": "EKS có thật nhưng chết từ 2023-08, đã chuyển sang ECS"},
            {"t": "Event-driven + Kafka", "lvl": "risk", "why": "Đã grep toàn repo: không có Kafka, không có SQS", "eta": "1–2 tuần",
             "doc": "nab-prep/04-kafka-event-driven-saga.md"},
            {"t": "Saga pattern / commit log", "lvl": "risk", "why": "Hỏi đích danh ở vòng EM. Nền đã có: bút toán ngược = compensating transaction", "eta": "3–5 ngày",
             "doc": "nab-prep/04-kafka-event-driven-saga.md"},
            {"t": "Tiếng Anh — vòng EM 100% English", "lvl": "risk", "why": "Rủi ro lớn nhất. Đã có lộ trình riêng ở ielts-target-5-5", "eta": "dài hạn",
             "doc": "nab-prep/05-english-interview.md"},
            {"t": "LeetCode live coding tới hard", "lvl": "warn", "why": "Có 38 bài cả Java lẫn Python, nhưng đọc ≠ gõ tay"},
            {"t": "Mentor / định hướng kỹ thuật", "lvl": "ok", "why": "ADR/RFC thật, bakeoff 43 run tự bác bỏ RFC của mình"},
            {"t": "Agile, code review team quốc tế", "lvl": "ok", "why": "Có story thật về review phát hiện hai bug che lấp nhau"},
        ],
    },
    "katalon": {
        "name": "Katalon",
        "role": "Senior / Lead Software Engineer · HCM",
        "blurb": "Product company về test automation. TrueTest là sản phẩm lõi — AI sinh test case "
                 "từ hành vi người dùng thật. Đây là chỗ nền AI/agent của bạn phát huy.",
        "stack": ["Java", "Python", "JavaScript/TypeScript", "Playwright", "Spark", "Kafka",
                  "Kubernetes", "LLM / agent"],
        "note": "Vòng take-home là vòng quyết định. Viết bằng Java — đó là bằng chứng duy nhất họ "
                "có về \"strong Java\".",
        "rounds": [
            {"t": "Screening với Talent Acquisition", "d": "Logistics, lương, notice, why Katalon"},
            {"t": "Technical", "d": "Java, clean code, design pattern, DSA"},
            {"t": "System design", "d": "TrueTest journey mining, distributed test execution, analytics"},
            {"t": "Take-home", "d": "Vòng quyết định — viết Java, sẽ bị hỏi lại live"},
            {"t": "Behavioral / lead signal", "d": "6 STAR story, mỗi story phải có số liệu"},
        ],
        "req": [
            {"t": "Strong Java", "lvl": "warn", "why": "Requirement #1 trong JD. Cần gõ tay, tắt Copilot"},
            {"t": "AI agent, memory, tool calling, flow control", "lvl": "ok", "why": "Hai dự án thật, RFC-bakeoff 43 run, số đo thật",
             "doc": "katalon-prep/AI-STACK-INTERVIEW-ANSWERS.md"},
            {"t": "Vector / embedding / rerank", "lvl": "ok", "why": "Qdrant 3 collection, đã đo, biết cả technical debt của mình",
             "doc": "katalon-prep/AI-STACK-INTERVIEW-ANSWERS.md"},
            {"t": "System design on-domain", "lvl": "ok", "why": "6 bài đầy đủ + bản đồ 7 họ bài",
             "doc": "katalon-prep/katalon-system-design/README.md"},
            {"t": "Python / PySpark", "lvl": "warn", "why": "Python mạnh; Spark chủ động để sau"},
            {"t": "Playwright / TypeScript / CDP", "lvl": "risk", "why": "JD ghi hard requirement. Không có dòng code nào trong workspace", "eta": "2–3 tuần"},
            {"t": "Browser automation & instrumentation", "lvl": "risk", "why": "Cùng gap với Playwright — cần một project TS riêng", "eta": "2–3 tuần"},
            {"t": "Clean code / SOLID / design pattern", "lvl": "ok", "why": "Module 01 với 61 test, có story bug che lấp nhau"},
            {"t": "Distributed systems, Kafka, resilience", "lvl": "warn", "why": "Lý thuyết vững, code drill có; Kafka thật thì chưa"},
            {"t": "Postgres depth", "lvl": "ok", "why": "Lab EXPLAIN, index, partition — đã chạy và cố tình phá"},
        ],
    },
}

FAMS = [
    {"k": "A", "n": "Đếm & tổng hợp theo thời gian",
     "sig": "Ghi nặng, đọc nhẹ, nhưng đọc theo khoảng thời gian bất kỳ",
     "core": "Bucket thời gian + rollup phân tầng (phút→giờ→ngày) + phân rã khoảng lúc query",
     "trap": "Cardinality explosion; lưu tỉ lệ thay vì count; bucket theo ingest-time",
     "doc": "katalon-prep/katalon-system-design/04-event-counting-10k.md"},
    {"k": "B", "n": "Điều phối tác vụ & chia tài nguyên hữu hạn",
     "sig": "Nhiều bên tranh nhau một pool hữu hạn",
     "core": "Quota cứng + hàng đợi có trọng số (WFQ/DRR) + priority & preemption + LPT bin-packing",
     "trap": "Đọc thành bài throughput; FIFO chung gây head-of-line blocking; priority tuyệt đối gây starvation",
     "doc": "katalon-prep/katalon-system-design/02-distributed-test-execution.md"},
    {"k": "C", "n": "Thu thập & xử lý luồng sự kiện",
     "sig": "Dữ liệu đến liên tục từ nguồn mình không kiểm soát",
     "core": "Backpressure + at-least-once + sink idempotent + partition key giữ ordering đúng phạm vi",
     "trap": "Hàng đợi vô hạn; hot partition; PII rời client trước khi redact",
     "doc": "katalon-prep/katalon-system-design/01-truetest-journey-mining.md"},
    {"k": "D", "n": "Hệ thống có LLM trong vòng lặp",
     "sig": "Có bước không tất định nằm giữa pipeline",
     "core": "Plan tất định, LLM chỉ nằm trong tool; schema ràng buộc; validation gate",
     "trap": "Để LLM quyết flow; tin self-reported confidence; không có eval harness",
     "doc": "katalon-prep/katalon-llm-migration-va-scale.md"},
    {"k": "E", "n": "Tìm kiếm ngữ nghĩa / RAG",
     "sig": "Truy vấn theo ý nghĩa, không theo khoá chính xác",
     "core": "Embedding + vector index + rerank (bi-encoder lọc, cross-encoder xếp hạng)",
     "trap": "Cắt top_k quá sớm; truncate embedding của model không phải MRL",
     "doc": "katalon-prep/AI-STACK-INTERVIEW-ANSWERS.md"},
    {"k": "F", "n": "Độ tin cậy & chống lỗi lan",
     "sig": "Có phụ thuộc ngoài có thể chậm hoặc chết",
     "core": "Timeout theo tầng + retry có jitter + circuit breaker + bulkhead + DLQ",
     "trap": "Retry không jitter gây thundering herd; nuốt lỗi thành dead code",
     "doc": "katalon-prep/katalon-self-questions/01-deadlock.md"},
    {"k": "G", "n": "Quản lý đồng thời trên trạng thái chia sẻ",
     "sig": "Đọc → tính → ghi, và giữa đọc với ghi có người khác chen vào",
     "core": "Ranh giới transaction đúng + khoá + idempotency là tiền đề của retry + append-only",
     "trap": "Nhầm write skew với lost update; synchronized JVM-local khi chạy nhiều instance",
     "doc": "katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md"},
]

AXES = [
    {"q": "Ghi nặng hay đọc nặng?",
     "a": "Ghi ≫ đọc → hấp thụ burst, ghi bất đồng bộ. Đọc ≫ ghi → cache, read replica, denormalize"},
    {"q": "Đọc theo điểm hay theo khoảng?",
     "a": "Theo khoảng thời gian → BẮT BUỘC pre-aggregation + rollup phân tầng. Đây là chỗ đa số ứng viên trượt"},
    {"q": "Có tài nguyên hữu hạn mà nhiều bên tranh nhau không?",
     "a": "Có → bài fairness, không phải bài throughput. Quota + hàng đợi có trọng số + preemption"},
    {"q": "Kết quả cần chính xác tuyệt đối hay xấp xỉ được?",
     "a": "Tuyệt đối → giữ raw + idempotent + job đối soát. Xấp xỉ → mở khoá sketch/sampling/top-K"},
    {"q": "Có đọc rồi tính rồi ghi dựa trên cái vừa đọc không?",
     "a": "Có → bài concurrency, không phải bài performance. Trục dễ bỏ sót nhất — không phụ thuộc quy mô"},
]


# ---- lộ trình Java System Design 6 tháng (java-system-design/) -----------------------------
# Khai tay như FAMS/AXES; anchor được tính bằng gh_slug từ đúng tiêu đề trong file .md và
# main() kiểm tra từng anchor có thật trong HTML đã render — đổi tiêu đề mà quên sửa ở đây thì build báo.
SDR_DIR = "java-system-design/"
SDR = {
    "doc": SDR_DIR + "00-lo-trinh-6-thang.md",
    "phases": [
        {"n": 1, "t": "Nền tảng", "w": "Tuần 1–4", "h": "Giai đoạn 1 — Nền tảng (tuần 1–4)",
         "learn": "Ước lượng tải, networking và API, database sâu, caching và nhất quán",
         "lab": "Rate limiter Redis + Lua · lost update → @Version · cache stampede",
         "p": "Nhóm 1 bằng JMH, P08–P10, P15–P19", "s": "V1–V5 · L0/L1 100k users",
         "impl": SDR_DIR + "10-implement-gd1-nen-tang.md",
         "ms": "URL shortener + Rate limiter trong 45 phút"},
        {"n": 2, "t": "Dữ liệu và hệ phân tán", "w": "Tuần 5–10",
         "h": "Giai đoạn 2 — Dữ liệu và hệ phân tán (tuần 5–10)",
         "learn": "Replication, partitioning, Saga và outbox, Kafka sâu, đồng thuận",
         "lab": "Kafka + outbox + DLQ với Testcontainers · capstone bắt đầu tuần 9",
         "p": "P20 Kafka consumer xử lý từng message", "s": "V6–V7 · L2 1M users",
         "impl": SDR_DIR + "20-implement-gd2-du-lieu-phan-tan.md",
         "ms": "Giải thích Saga/outbox/Kafka không cần tài liệu"},
        {"n": 3, "t": "Microservices, cloud, vận hành", "w": "Tuần 11–16",
         "h": "Giai đoạn 3 — Microservices, cloud và vận hành (tuần 11–16)",
         "learn": "DDD, resilience, observability, security, AWS multi-AZ, DR",
         "lab": "Capstone: load test, thử phá, deploy lên cloud",
         "p": "P11–P14, JFR + async-profiler, checklist review", "s": "V8–V10 · L3 10M users",
         "impl": SDR_DIR + "30-implement-gd3-microservices-cloud.md",
         "ms": "Capstone chạy trên cloud, có số load test"},
        {"n": 4, "t": "Phỏng vấn và ứng tuyển", "w": "Tuần 17–24",
         "h": "Giai đoạn 4 — Luyện phỏng vấn và ứng tuyển (tuần 17–24)",
         "learn": "3–4 đề mỗi tuần có bấm giờ, mock tiếng Anh, STAR, CV có số",
         "lab": "Mock interview mỗi tuần",
         "p": "1 câu chuyện STAR về hiệu năng", "s": "Drill: một hệ thống, ba quy mô",
         "ms": "CV mới, 6–8 STAR, nộp đơn đợt đầu"},
    ],
    "tracks": [
        {"k": "P", "t": "Code Java chậm dưới tải cao",
         "d": "20 anti-pattern, 16 có code chạy được và số đo thật. Kiến trúc tốt không cứu được "
              "code giữ connection, thread, carrier quá lâu.",
         "doc": SDR_DIR + "01-java-code-cham-duoi-tai-cao.md",
         "facts": [["P09 trên Spring Boot + Hikari thật, 250 rps", "p99 11,1 s → 103 ms"],
                   ["P10 gọi đối tác không timeout", "p99 3,1 s → 5 ms"],
                   ["P12 virtual thread pinning, JDK 21", "5,0 s → 23 ms"],
                   ["P15 N+1, trang 100 đơn hàng", "101 → 2 query"],
                   ["P20 Kafka consumer chậm, Kafka thật", "chưa xong sau 45 s → 1,3 s"]]},
        {"k": "S", "t": "Vẽ hệ thống 100k → 1M → 10M users",
         "d": "Đổi users ra DAU → CCU → RPS, chọn bậc kiến trúc L0–L4, 10 bản vẽ từ một máy tới "
              "microservices, trả lời câu \"tải tăng 10 lần thì sao\".",
         "doc": SDR_DIR + "02-ve-he-thong-100k-1m-10m.md",
         "facts": [["Công thức", "users → DAU → CCU → RPS"],
                   ["App ngân hàng vs app chat", "CCU chênh 12 lần"],
                   ["10M users ngân hàng", "ghi vẫn vừa 1 Postgres"],
                   ["Lộ trình vẽ", "V1 → V10 + drill"]]},
    ],
    "ladder": [
        {"u": "100k", "ccu": "~1.200", "rps": "~200", "lv": "L1",
         "arch": "Modular monolith × 2, Postgres Multi-AZ, Redis, CDN",
         "h": "3.2 L1 — 100k users: sẵn sàng trước, tải sau"},
        {"u": "1M", "ccu": "~12.000", "rps": "~2.000", "lv": "L2",
         "arch": "Autoscale, read replica, cache-aside, outbox → Kafka",
         "h": "3.3 L2 — 1M users: tách đọc, tách việc chậm"},
        {"u": "10M", "ccu": "~120.000", "rps": "~20.000", "lv": "L3",
         "arch": "Microservices theo bounded context, CQRS, CDC, multi-AZ + DR",
         "h": "3.4 L3 — 10M users: microservices có lý do"},
    ],
    "ms": [
        {"w": 4, "t": "URL shortener + Rate limiter trong 45 phút; tính CCU/RPS 100k–10M; JMH nhóm 1; bản vẽ V2"},
        {"w": 10, "t": "Giải thích replication, Saga/outbox, Kafka delivery; lab Kafka + outbox chạy; bản vẽ V6"},
        {"w": 12, "t": "Design doc đầu tiên được review; load test tìm và sửa 1 anti-pattern có số trước/sau"},
        {"w": 16, "t": "Capstone trên cloud, có số load test và README; bản vẽ V10; qua checklist review track P"},
        {"w": 20, "t": "20+ đề, 4+ mock interview; drill “một hệ thống, ba quy mô” 3 lần"},
        {"w": 24, "t": "CV mới, 6–8 câu chuyện STAR, đã nộp đơn đợt đầu"},
    ],
}


def resolve_sdr_anchors(docs: dict[str, dict]) -> None:
    """Đổi tiêu đề ("h") thành anchor, và kiểm tra anchor có trong tài liệu đã render."""
    ladder_doc = SDR_DIR + "02-ve-he-thong-100k-1m-10m.md"
    for items, doc in ((SDR["phases"], SDR["doc"]), (SDR["ladder"], ladder_doc)):
        for it in items:
            it["a"] = gh_slug(it.pop("h"))
            it["doc"] = doc
            if doc not in docs or f'id="{it["a"]}"' not in docs[doc]["h"]:
                print(f"⚠ anchor #{it['a']} không có trong {doc} — sửa SDR trong build.py")
    for it in SDR["phases"] + SDR["tracks"]:
        for key in ("impl", "doc"):
            if it.get(key) and it[key] not in docs:
                print(f"⚠ {it[key]} không tồn tại — sửa SDR trong build.py")


def main() -> None:
    docs = collect_docs()
    resolve_sdr_anchors(docs)
    algos = collect_algos()
    if len(algos) != 38:
        print(f"⚠ đọc được {len(algos)} bài thuật toán, kỳ vọng 38 — kiểm tra bảng mục lục")

    series = collect_jsd_series(docs)
    tpl = (WEB / "app.template.html").read_text(encoding="utf-8")
    dump = lambda o: json.dumps(o, ensure_ascii=False, separators=(",", ":"))

    def render(media_base: str, standalone: bool) -> str:
        jsdv = {"dir": SDR_DIR, "series": [dict(x, base=jsd_base(x["id"], standalone)) for x in series]}
        return (tpl
                .replace("/*__JSDV__*/{}", dump(jsdv))
                .replace("/*__DOCS__*/{}", dump(docs))
                .replace("/*__ALGOS__*/[]", dump(algos))
                .replace("/*__CO__*/{}", dump(COMPANIES))
                .replace("/*__FAMS__*/[]", dump(FAMS))
                .replace("/*__AXES__*/[]", dump(AXES))
                .replace("/*__SDR__*/{}", dump(SDR))
                .replace('/*__MEDIA_BASE__*/""', dump(media_base)))

    out = render(MEDIA_BASE_ARTIFACT, False)
    (WEB / "algorithms-learning.html").write_text(out, encoding="utf-8")
    out = render(MEDIA_BASE_STANDALONE, True)

    standalone = ('<!doctype html>\n<html lang="vi">\n<head>\n<meta charset="utf-8">\n'
                  '<meta name="viewport" content="width=device-width,initial-scale=1">\n'
                  + out.split("<style>", 1)[0] +
                  "<style>" + out.split("<style>", 1)[1].split("</style>", 1)[0] + "</style>\n"
                  "</head>\n<body>\n"
                  + "</style>".join(out.split("</style>")[1:]) +
                  "\n</body>\n</html>\n")
    (WEB / "index.html").write_text(standalone, encoding="utf-8")

    kb = len(out.encode()) / 1024
    print(f"✓ {len(docs)} tài liệu · {len(algos)} bài thuật toán · "
          f"{sum(len(c['req']) for c in COMPANIES.values())} yêu cầu JD")
    print(f"  web/algorithms-learning.html  {kb:.0f} KB  → publish làm Artifact")
    print(f"  web/index.html                          → mở trực tiếp bằng trình duyệt")
    for x in series:
        print(f"  {x['id']}: {len(x['lessons'])} video (tab Video): {', '.join(v.get('code') or 'Ep%02d' % v['ep'] for v in x['lessons'])}"
              f" — media từ {jsd_base(x['id'], True)}")
    videos = [a for a in algos if "v" in a]
    if videos:
        print(f"  {len(videos)} bài có video: {', '.join(str(a['i']) for a in videos)}"
              " — khi publish Artifact, gửi kèm các file:")
        for a in videos:
            for kind in ("mp4", "gif"):
                if kind in a["v"]:
                    print(f"    {MEDIA_BASE_ARTIFACT}{a['v'][kind]}  ←  leetcode-38-bai-video/{a['v'][kind]}")


if __name__ == "__main__":
    main()
