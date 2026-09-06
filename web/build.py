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
import re
from pathlib import Path

import markdown

ROOT = Path(__file__).resolve().parent.parent
WEB = ROOT / "web"
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
        if any(part in SKIP for part in p.relative_to(ROOT).parts):
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
        head = 0 if rel == "README.md" else 1 if rel.startswith("nab-prep/") else 2
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
    return rows


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


def main() -> None:
    docs = collect_docs()
    algos = collect_algos()
    if len(algos) != 38:
        print(f"⚠ đọc được {len(algos)} bài thuật toán, kỳ vọng 38 — kiểm tra bảng mục lục")

    tpl = (WEB / "app.template.html").read_text(encoding="utf-8")
    dump = lambda o: json.dumps(o, ensure_ascii=False, separators=(",", ":"))
    out = (tpl
           .replace("/*__DOCS__*/{}", dump(docs))
           .replace("/*__ALGOS__*/[]", dump(algos))
           .replace("/*__CO__*/{}", dump(COMPANIES))
           .replace("/*__FAMS__*/[]", dump(FAMS))
           .replace("/*__AXES__*/[]", dump(AXES)))

    (WEB / "algorithms-learning.html").write_text(out, encoding="utf-8")

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


if __name__ == "__main__":
    main()
