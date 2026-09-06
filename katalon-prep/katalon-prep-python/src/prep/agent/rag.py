"""Module 09 — RAG. Katalon dung **pgvector tren Aurora PostgreSQL** (co trong JD).

Bai toan on-domain: agent sinh test case can tim **cac test case cu tuong tu** va
**mau DOM da gap** de bam theo, thay vi bia tu dau.

===========================================================================
DIEU QUAN TRONG NHAT O FILE NAY
===========================================================================
**Tim kiem vector MOT MINH lam rat te voi dinh danh chinh xac.**

`#submit-btn`, `data-testid="email-input"`, ma loi `ERR_CONN_REFUSED` — embedding
bien chung thanh vector "gan giong" `#submit-button`, `#submit_btn`, `#cancel-btn`.
Voi van xuoi thi "gan giong" la uu diem; voi **dinh danh** thi do la sai hoan toan:
`#submit-btn` va `#cancel-btn` gan nhau trong khong gian vector nhung nguoc nhau ve
y nghia — mot cai gui form, mot cai huy.

Ma domain cua Katalon thi day dinh danh: selector, ten test, ma loi, ten API.

Nen: **hybrid search** — vector cho y nghia, tu khoa cho dinh danh, gop diem lai.
Neu phong van hoi "RAG cua ban thiet ke the nao?", tra loi "embedding roi cosine" la
cau tra loi cua nguoi doc tutorial. Noi duoc doan tren la cau tra loi cua nguoi da lam.

Embedding o day la ham bam TAT DINH — khong goi mang, khong tai model. Muc dich la
hoc CO CHE va test duoc cach xep hang, khong phai do chat luong embedding.
"""

from __future__ import annotations

import hashlib
import math
import re
from collections.abc import Callable, Iterable, Sequence
from dataclasses import dataclass, field

EMBED_DIM = 64
_TOKEN = re.compile(r"[a-z0-9_\-#\[\]='\.]+")
# Giu lai `#`, `-`, `[`, `]`, `=` trong token: tach `#submit-btn` thanh "submit" + "btn"
# la vut di dung cai lam no la mot dinh danh duy nhat.


def tokenize(text: str) -> list[str]:
    return _TOKEN.findall(text.lower())


def _hash_into(vec: list[float], pieces: Iterable[str]) -> list[float]:
    for piece in pieces:
        digest = hashlib.blake2b(piece.encode(), digest_size=8).digest()
        idx = int.from_bytes(digest[:4], "big") % EMBED_DIM
        vec[idx] += 1.0 if digest[4] % 2 == 0 else -1.0
    norm = math.sqrt(sum(v * v for v in vec))
    return [v / norm for v in vec] if norm else vec


def embed_whole_token(text: str) -> list[float]:
    """Bam TUNG TOKEN NGUYEN vao mot chieu.

    LUU Y — day KHONG giong embedding that, va toi da nham cho nay khi viet test lan dau:
    vi `#cancel-btn` duoc bam nguyen ca cum, ham nay phan biet no voi `#submit-btn`
    HOAN HAO. Tuc la no hanh xu nhu tim kiem tu khoa, chu khong nhu tim kiem vector.

    Dung no de test co che xep hang thi duoc; dung no de ket luan "vector search on
    voi selector" thi la ket luan SAI. Xem `embed_subword`.
    """
    return _hash_into([0.0] * EMBED_DIM, tokenize(text))


def subword_pieces(token: str) -> list[str]:
    """Cat token thanh manh nho — mo phong cach tokenizer THAT lam viec.

    Moi tokenizer that (BPE/SentencePiece) deu cat `#submit-btn` thanh dai khai
    `#`, `sub`, `mit`, `-`, `btn`. Do la ly do embedding that KHONG the phan biet
    ro `#submit-btn` voi `#cancel-btn`: chung dung chung phan lon cac manh.
    """
    parts = re.split(r"[-_\[\]='\.#]+", token)
    pieces = [p for p in parts if p]
    out: list[str] = []
    for p in pieces:
        out.extend(p[i : i + 3] for i in range(0, len(p), 3))
    return out or [token]


def embed_subword(text: str) -> list[float]:
    """Embedding gia SAT VOI THUC TE hon: bam theo manh subword.

    Van khong phai embedding ngu nghia (tu dong nghia khong gan nhau). Nhung no tai
    tao dung **mot** tinh chat quan trong nhat cho bai toan nay: cac dinh danh co
    hinh dang giong nhau thi gan nhau trong khong gian vector — ke ca khi y nghia
    nguoc hau nhau.
    """
    pieces = [p for token in tokenize(text) for p in subword_pieces(token)]
    return _hash_into([0.0] * EMBED_DIM, pieces)


# Mac dinh dung subword: sat thuc te hon. Doi bang `VectorIndex(..., embedder=...)`.
embed = embed_subword


def cosine(a: Sequence[float], b: Sequence[float]) -> float:
    return sum(x * y for x, y in zip(a, b, strict=True))


@dataclass(frozen=True, slots=True)
class Chunk:
    chunk_id: str
    text: str
    source: str
    metadata: dict[str, str] = field(default_factory=dict)


def chunk_dom(html: str, max_chars: int = 300, overlap: int = 50) -> list[Chunk]:
    """Cat theo RANH GIOI THE, khong cat theo so ky tu co dinh.

    Cat mu theo ky tu se xe doi mot the: `<button id="sub` | `mit-btn">`. Khi do
    selector bien mat khoi ca hai manh — khong manh nao tim duoc no nua.

    `overlap` de mot the nam vat ranh gioi van con nguyen trong it nhat mot manh.
    Cai gia: du lieu phinh ra va ket qua tim kiem co ban trung. Voi HTML thi dang,
    voi van xuoi dai thi can can nhac.
    """
    parts = re.split(r"(?=<)", html)
    chunks: list[Chunk] = []
    buffer = ""
    for part in parts:
        if len(buffer) + len(part) > max_chars and buffer:
            chunks.append(Chunk(f"c{len(chunks)}", buffer.strip(), "dom"))
            buffer = buffer[-overlap:] if overlap else ""
        buffer += part
    if buffer.strip():
        chunks.append(Chunk(f"c{len(chunks)}", buffer.strip(), "dom"))
    return chunks


@dataclass(frozen=True, slots=True)
class Hit:
    chunk: Chunk
    score: float
    vector_score: float = 0.0
    keyword_score: float = 0.0

    def why(self) -> str:
        """Giai thich vi sao no duoc chon. Bat buoc phai co, khong phai tuy chon.

        RAG hong thi trieu chung la "cau tra loi sai" — nhung nguyen nhan co the o
        khau tim (lay nham doan), o khau xep hang, hoac o khau sinh. Khong ghi lai
        diem tung thanh phan thi khong bao gio tach duoc ba kha nang do.
        """
        return f"tong={self.score:.3f} (vector={self.vector_score:.3f} tu_khoa={self.keyword_score:.3f})"


class VectorIndex:
    """Chi so trong bo nho. Doi ung that la mot bang pgvector.

    Bay tuong duong o pgvector, phai biet: **index HNSW/IVFFlat la GAN DUNG.** No co
    the bo sot ban ghi dung nhat de doi lay toc do. Voi bang nho thi seq scan lai
    chinh xac hon. `EXPLAIN` van la cong cu de kiem tra — y het module 05 ben Java.
    """

    def __init__(
        self,
        chunks: Iterable[Chunk] = (),
        embedder: Callable[[str], list[float]] = embed,
    ) -> None:
        # Tiem ham embed vao: doi sang embedding that chi la doi mot tham so, va
        # test doi chieu duoc hai cach bam ma khong phai sua class.
        self._embed = embedder
        self._chunks: list[Chunk] = []
        self._vectors: list[list[float]] = []
        self._df: dict[str, int] = {}
        for c in chunks:
            self.add(c)

    def add(self, chunk: Chunk) -> None:
        self._chunks.append(chunk)
        self._vectors.append(self._embed(chunk.text))
        for token in set(tokenize(chunk.text)):
            self._df[token] = self._df.get(token, 0) + 1

    def __len__(self) -> int:
        return len(self._chunks)

    def search_vector(self, query: str, k: int = 5) -> list[Hit]:
        q = self._embed(query)
        scored = [(cosine(q, v), c) for v, c in zip(self._vectors, self._chunks, strict=True)]
        scored.sort(key=lambda t: -t[0])
        return [Hit(c, s, vector_score=s) for s, c in scored[:k]]

    def search_keyword(self, query: str, k: int = 5) -> list[Hit]:
        """Cham diem kieu TF-IDF don gian — du de bat dinh danh chinh xac.

        Token hiem (idf cao) duoc uu tien: `#submit-btn` chi xuat hien o mot doan thi
        doan do phai len dau, du van canh xung quanh khong "giong" cau truy van.
        """
        q_tokens = set(tokenize(query))
        n = max(1, len(self._chunks))
        hits: list[Hit] = []
        for chunk in self._chunks:
            tokens = tokenize(chunk.text)
            if not tokens:
                continue
            score = 0.0
            for token in q_tokens:
                tf = tokens.count(token) / len(tokens)
                if tf:
                    score += tf * math.log(n / (1 + self._df.get(token, 0)) + 1)
            if score > 0:
                hits.append(Hit(chunk, score, keyword_score=score))
        hits.sort(key=lambda h: -h.score)
        return hits[:k]

    def search_hybrid(self, query: str, k: int = 5, alpha: float = 0.5) -> list[Hit]:
        """Gop hai bang xep hang bang **Reciprocal Rank Fusion**.

        Vi sao RRF chu khong cong thang diem: diem cosine nam trong [-1,1] con diem
        TF-IDF khong co tran. Cong thang thi thanh phan nao co thang do lon hon se
        nuot thanh phan kia — va no doi theo du lieu, nen hom nay dung mai lai sai.
        RRF chi dung THU HANG nen mien nhiem voi chuyen thang do.

        `alpha` = trong so cua nhanh vector. 0.5 la diem khoi dau hop ly, nhung day
        la **tham so phai tinh chinh bang bo vang cua chinh ban**, khong phai hang so
        vu tru. Domain nhieu dinh danh thi ha alpha xuong.
        """
        rank_v = {h.chunk.chunk_id: i for i, h in enumerate(self.search_vector(query, k * 3))}
        rank_k = {h.chunk.chunk_id: i for i, h in enumerate(self.search_keyword(query, k * 3))}
        by_id = {c.chunk_id: c for c in self._chunks}

        fused: list[Hit] = []
        for chunk_id in set(rank_v) | set(rank_k):
            v = alpha / (60 + rank_v[chunk_id]) if chunk_id in rank_v else 0.0
            kw = (1 - alpha) / (60 + rank_k[chunk_id]) if chunk_id in rank_k else 0.0
            fused.append(Hit(by_id[chunk_id], v + kw, vector_score=v, keyword_score=kw))
        fused.sort(key=lambda h: -h.score)
        return fused[:k]


def recall_at_k(retrieved: Sequence[Hit], relevant_ids: Sequence[str], k: int) -> float:
    """Do khau TIM tach rieng khoi khau SINH.

    Rat quan trong ve ky thuat do luong: neu recall@k = 0.4 thi 60% cau hoi khong
    bao gio co co hoi tra loi dung, du model co gioi den may. Toi uu prompt luc do
    la vo ich. Do rieng hai khau moi biet nen sua o dau.
    """
    if not relevant_ids:
        return 1.0
    top = {h.chunk.chunk_id for h in retrieved[:k]}
    return len(top & set(relevant_ids)) / len(set(relevant_ids))


def build_context(hits: Sequence[Hit], max_chars: int = 2000) -> str:
    """Ghep ket qua thanh context, CO tran va CO ghi nguon.

    Hai chi tiet nho ma quan trong:
      * Tran: nhoi 50 doan vao prompt lam moi luot dat gap boi, va lam nhieu cac doan
        that su lien quan ("lost in the middle" — model chu y kem o giua context dai).
      * Ghi nguon: model trich dan duoc thi nguoi dung kiem tra duoc. Khong co nguon
        thi khong the phan biet cau tra loi that voi cau tra loi bia.
    """
    parts: list[str] = []
    used = 0
    for i, h in enumerate(hits):
        block = f"[{i + 1}] (nguon: {h.chunk.source}#{h.chunk.chunk_id})\n{h.chunk.text}"
        if used + len(block) > max_chars:
            break
        parts.append(block)
        used += len(block)
    return "\n\n".join(parts)
