"""Đổi chữ trên màn hình thành chữ cho Kokoro đọc khi câu không có `say`.

Các quy tắc lấy từ lần kiểm bộ tách âm của Kokoro (espeak-ng) ngày 07/10/2026, ghi ở
java-system-design/video-gd1/00-ke-hoach-va-lich-su.md mục 5: dấu gạch dải số bị bỏ, số thập phân bị ngắt ở dấu
chấm, `~` đọc thành "tilde", `→` thành "right arrow", đơn vị ms/µs/KB đọc từng chữ cái, vài chữ viết tắt đọc sai.
Câu có `say` thì giữ nguyên `say`.
"""
from __future__ import annotations

import re

# chữ viết tắt / tên riêng Kokoro đọc sai → cách viết cho đúng (đã kiểm phiên âm)
WORDS = {
    "OIDC": "O I D C", "SLO": "S L O", "draw.io": "draw dot I O", "DDIA": "D D I eigh", "PACELC": "pass-elk", "ReDoS": "ree-doss", "DDoS": "dee-doss",
    "SaaS": "sass", "RAM": "ram", "Alex Xu": "Alex Shoo", "regex": "reg-ex", "memtable": "mem table",
    "idempotency": "idem-potency", "Idempotency": "Idem-potency", "idempotent": "idem-potent",
    "I/O": "I O", "HTTP/1.1": "HTTP one point one", "HTTP/2": "HTTP two", "HTTP/3": "HTTP three",
    "CCU/RPS": "CCU and RPS", "WebSocket/SSE": "WebSocket or SSE", "Ho Chi Minh City": "Ho Chee Minh City",
    "INCR": "increment", "PEXPIRE": "P expire", "X-Api-Key": "X API key", "O(n²)": "O of n squared",
    "O(n)": "O of n", "-Xmx": "X M X ", "my-work/": "my work", "vs": "versus", "e.g.": "for example",
    "i.e.": "that is",
    # giai đoạn 2 (kiểm phiên âm 08/10/2026): ISR đọc "isser", eKYC "ee-kick", etcd "etkd", Tết thành chuỗi chữ cái
    "ISR": "I S R", "eKYC": "e K Y C", "etcd": "et-see-dee", "retriable": "retry able", "Retriable": "Retry able",
    "Tết": "Tet",
}
UNITS = {
    "ns/op": "nanoseconds per op", "B/op": "bytes per op", "req/s": "requests per second",
    "Mbit/s": "megabits per second", "MB/s": "megabytes per second", "GB/s": "gigabytes per second",
    "ops/s": "operations per second", "ms": "milliseconds", "µs": "microseconds", "ns": "nanoseconds",
    "KB": "kilobytes", "MB": "megabytes", "GB": "gigabytes", "TB": "terabytes", "B": "bytes",
    "s": "seconds", "h": "hours",
}
SUPER = str.maketrans("⁰¹²³⁴⁵⁶⁷⁸⁹", "0123456789")
ORD = {"2": "second", "3": "third", "4": "fourth", "5": "fifth", "6": "sixth", "7": "seventh", "8": "eighth",
       "9": "ninth", "10": "tenth"}
SCALE = {"k": "thousand", "K": "thousand", "M": "million", "B": "billion"}


def _decimal(m: re.Match) -> str:
    whole, frac = m.group(1), m.group(2)
    return f"{whole} point {' '.join(frac)}"


def _power(m: re.Match) -> str:
    base, exp = m.group(1), m.group(2).translate(SUPER)
    return f"{base} to the {ORD.get(exp, exp + 'th')}"


def speakable(text: str) -> str:
    t = text
    for k in sorted(WORDS, key=len, reverse=True):
        t = re.sub(r"(?<![\w-])" + re.escape(k) + r"(?![\w])", WORDS[k], t)
    # chữ "A" đứng riêng (Version A, profile A, AZ-a…) bị đọc thành mạo từ → "eigh" (cách của repo IELTS)
    t = re.sub(r"\b(Version|version|Profile|profile|Family|family|Part|part|Option|option|Plan|plan|Step|step|"
               r"Exercise|exercise|Lab|lab|Group|group|level|Level) A\b", r"\1 eigh", t)
    t = re.sub(r"\bAZ-a\b", "AZ eigh", t)
    # "A" viết hoa giữa câu là tên (Client A, relay A, then A pauses, fix A), mạo từ giữa câu luôn viết
    # thường; đầu câu thì để nguyên vì "A poll returns…" là mạo từ. "A's" Kokoro đã đọc đúng.
    t = re.sub(r"(?<=[^\s.!?“\"(])(\s+)A\b(?![’'/])", r"\1eigh", t)
    # mã của lộ trình: P01–P07 → P one to P seven; D09 → D nine; Ep03 → episode three
    t = re.sub(r"\b([PD])0(\d)\b", r"\1 \2", t)
    t = re.sub(r"\bEp0?(\d+)\b", r"episode \1", t)
    # lũy thừa: 10^5, 10⁵, 62^7
    t = re.sub(r"\b(\d+)\^(\d+)", _power, t)
    t = re.sub(r"\b(\d+)([⁰¹²³⁴⁵⁶⁷⁸⁹]+)", _power, t)
    # dải: 2–3, P01–P07, V1–V5 (gạch ngắn "–" giữa hai số / hai mã); gạch dài "—" là ngắt câu → dấu phẩy
    t = re.sub(r"(?<=[\w%])\s?–\s?(?=[\w~≈])", " to ", t)
    t = re.sub(r"\s*—\s*", ", ", t)
    # số có hậu tố k/M/B viết liền: 100k, 1.5M, 6.7k (có dấu cách thì là đơn vị: "600 B" = bytes)
    # mã lab "lab 5B", "lab 10B" là tên bài, không phải 5 tỷ hay 5 byte → "lab 5-B" (đọc "five B")
    t = re.sub(r"\b([Ll]abs?) (\d+)([BC])\b", r"\1 \2-\3", t)
    t = re.sub(r"\b(\d+(?:\.\d+)?)([kKMB])\b(?!/)", lambda m: f"{m.group(1)} {SCALE[m.group(2)]}", t)
    # thập phân: 0.64 → 0 point 6 4 (dấu chấm làm Kokoro ngắt câu)
    t = re.sub(r"\b(\d+)\.(\d+)\b", _decimal, t)
    # "GB/day" → "GB a day" trước khi đổi đơn vị
    t = re.sub(r"(?<=[A-Za-z])/(day|year|month|hour|minute)\b", r" a \1", t)
    # đơn vị đứng sau số
    for u in sorted(UNITS, key=len, reverse=True):
        t = re.sub(r"(?<=\d)\s?" + re.escape(u) + r"(?![\w/])", " " + UNITS[u], t)
    t = re.sub(r"(?<=[a-z])/s\b", " per second", t)          # sau đơn vị: "tests/s", "pings/s"
    t = re.sub(r"(to the \w+) s\b", r"\1 seconds", t)
    t = re.sub(r"(?<=\d)\s?×(?=\s|$|[),.;])", " times", t)       # 2.4× → times
    t = re.sub(r"\s?×\s?", " times ", t)
    t = t.replace("≈", " about ").replace("~", " about ").replace("→", " to ").replace("÷", " divided by ")
    t = re.sub(r"(?<=\s)\+(?=\s)", "plus", t)
    t = re.sub(r"(?<=\d)\+", " plus", t)
    t = t.replace("≤", " at most ").replace("≥", " at least ").replace("≠", " not equal to ").replace("λ", "lambda")
    t = re.sub(r"(?<=\s)>(?=\s)", "greater than", t)          # R + W > N (dấu > đứng riêng bị bỏ khi đọc)
    t = re.sub(r"(?<=\s)<(?=\s)", "less than", t)
    # tên cấu hình có dấu chấm (max.poll.records, min.insync.replicas): dấu chấm làm Kokoro ngắt câu
    t = re.sub(r"\b[a-z]+(?:\.[a-z]+)+\b", lambda m: m.group(0).replace(".", " "), t)
    t = t.replace(" / ", " or ").replace("…", "...")
    return re.sub(r"\s{2,}", " ", t).strip()
