"""Module 09 (RAG) — vi sao domain cua Katalon can hybrid search.

Test dang gia nhat la `test_vector_va_tu_khoa_HONG_NGUOC_CHIEU_NHAU___do_do_can_ca_hai`.

Doc docstring cua no truoc: no ghi lai viec toi dinh chung minh mot dieu, chay thu
ra khac, va phai viet lai theo so do duoc. Ket luan cuoi cung manh hon ket luan
toi dinh san — nhung chi vi da chay chu khong chi doc.
"""

from __future__ import annotations

import pytest

from prep.agent.rag import (
    Chunk,
    VectorIndex,
    build_context,
    chunk_dom,
    cosine,
    embed,
    recall_at_k,
    subword_pieces,
    tokenize,
)

CORPUS = [
    Chunk("t1", "Test dang nhap: click #submit-btn roi kiem tra #welcome-banner hien ra",
          "test-cu", {"suite": "auth"}),
    Chunk("t2", "Test huy don: click #cancel-btn roi kiem tra don hang bi huy",
          "test-cu", {"suite": "order"}),
    Chunk("t3", "Test thanh toan: nhap the vao [data-testid='card-input'] roi xac nhan",
          "test-cu", {"suite": "payment"}),
    Chunk("t4", "Huong dan: khi nut gui form khong phan hoi, kiem tra lai su kien onclick",
          "tai-lieu", {}),
    Chunk("t5", "Ma loi ERR_CONN_REFUSED nghia la server tu choi ket noi",
          "tai-lieu", {}),
]


def test_tokenize_GIU_NGUYEN_dinh_danh_khong_xe_le():
    """Tach `#submit-btn` thanh 'submit' + 'btn' la vut di cai lam no duy nhat."""
    assert "#submit-btn" in tokenize("click #submit-btn ngay")
    assert "[data-testid='card-input']" in tokenize("nhap vao [data-testid='card-input']")


def test_embed_TAT_DINH_va_da_chuan_hoa():
    a, b = embed("test dang nhap"), embed("test dang nhap")
    assert a == b
    assert cosine(a, a) == pytest.approx(1.0)
    assert embed("") == [0.0] * 64  # chuoi rong khong lam no


def test_vector_va_tu_khoa_HONG_NGUOC_CHIEU_NHAU___do_do_can_ca_hai(capsys):
    """>>> TEST QUAN TRONG NHAT FILE NAY, va la cau tra loi mang di phong van <<<

    Toi dinh viet test nay de chung minh "vector search on voi selector". CHAY THU
    thi khong ra nhu vay — voi corpus nay vector van xep dung. Nen day la ket qua
    THAT SU do duoc, khong phai ket luan toi dinh san:

        truy van          | tu khoa      | vector (subword)
        ------------------|--------------|---------------------------
        '#cancel-btn'     | t2  (dung)   | t2 0.369, roi t1 0.265  <- t1 la #submit-btn!
        '#submit_btn'     | RONG         | t1 0.265  (dung)
        '#submitBtn'      | RONG         | t1 0.265  (dung)

    Hai kieu hong NGUOC CHIEU nhau:

      * **Tu khoa**: chinh xac tuyet doi khi khop, nhung mot bien the chinh ta
        (`_` thay `-`, camelCase) lam no tra ve **RONG HOAN TOAN**. Khong phai
        "kem hon", ma la **khong co gi**.
      * **Vector**: chiu duoc bien the chinh ta, nhung xep `#submit-btn` o 72% diem
        cua `#cancel-btn` — hai thu **nguoc hau nhau ve y nghia** (mot cai gui form,
        mot cai huy). Trong corpus that hang chuc ngan doan thi rac do lap kin top-k.

    Do la ly do that su can hybrid — khong phai vi "hybrid tot hon", ma vi hai
    phuong phap co diem mu KHONG chong len nhau.
    """
    index = VectorIndex(CORPUS)

    # --- huong 1: dinh danh chinh xac -> tu khoa thang ve do chinh xac ---
    exact_kw = index.search_keyword("#cancel-btn", k=3)
    exact_vec = index.search_vector("#cancel-btn", k=3)
    assert [h.chunk.chunk_id for h in exact_kw] == ["t2"], "tu khoa chi tra ve dung doan chua no"
    assert exact_vec[0].chunk.chunk_id == "t2"
    # ...nhung vector keo theo ca #submit-btn voi diem rat gan:
    assert exact_vec[1].chunk.chunk_id == "t1"
    assert exact_vec[1].score / exact_vec[0].score > 0.7, "doan KHONG lien quan dat >70% diem"

    # --- huong 2: bien the chinh ta -> tu khoa CHET, vector cuu ---
    for variant in ("#submit_btn", "#submitBtn"):
        assert index.search_keyword(variant, k=3) == [], f"{variant}: tu khoa tra ve RONG"
        assert index.search_vector(variant, k=1)[0].chunk.chunk_id == "t1", f"{variant}: vector van tim ra"

    with capsys.disabled():
        print("\n  '#cancel-btn'  tu khoa=['t2']  vector="
              f"{[(h.chunk.chunk_id, round(h.score, 3)) for h in exact_vec[:2]]}")
        print("  '#submit_btn'  tu khoa=[]      vector="
              f"{[(h.chunk.chunk_id, round(h.score, 3)) for h in index.search_vector('#submit_btn', 1)]}")

    # --- hybrid: giu duoc do chinh xac o huong 1 ---
    assert index.search_hybrid("#cancel-btn", k=1)[0].chunk.chunk_id == "t2"


def test_embedding_SUBWORD_lam_mo_ranh_gioi_giua_cac_dinh_danh():
    """Co che ben duoi test tren, do bang so.

    Tokenizer that (BPE/SentencePiece) cat `#submit-btn` -> [sub, mit, btn] va
    `#cancel-btn` -> [can, cel, btn]. Chung dung chung manh `btn` nen KHONG bao gio
    truc giao nhau. Bam nguyen ca token thi lai truc giao hoan toan — nhung do khong
    phai cach embedding that hoat dong, nen ket luan rut ra tu no se sai.
    """
    from prep.agent.rag import embed_subword, embed_whole_token

    assert subword_pieces("#submit-btn") == ["sub", "mit", "btn"]
    assert subword_pieces("#cancel-btn") == ["can", "cel", "btn"]

    sub = cosine(embed_subword("#submit-btn"), embed_subword("#cancel-btn"))
    whole = cosine(embed_whole_token("#submit-btn"), embed_whole_token("#cancel-btn"))
    assert sub == pytest.approx(0.333, abs=0.01), "subword: gan nhau du y nghia nguoc"
    assert whole == pytest.approx(0.0, abs=0.01), "bam nguyen token: truc giao - KHONG giong that"


def test_tu_khoa_uu_tien_token_HIEM():
    """`ERR_CONN_REFUSED` chi co o mot doan -> phai len dau du van canh khong giong."""
    index = VectorIndex(CORPUS)
    hits = index.search_keyword("ERR_CONN_REFUSED la gi", k=1)
    assert hits[0].chunk.chunk_id == "t5"


def test_hybrid_dung_THU_HANG_nen_mien_nhiem_voi_thang_do():
    """Vi sao RRF chu khong cong thang diem.

    Diem cosine trong [-1,1]; diem TF-IDF khong co tran. Cong thang thi nhanh nao co
    thang do lon hon se nuot nhanh kia — va no doi theo du lieu, nen hom nay dung
    mai lai sai.
    """
    index = VectorIndex(CORPUS)
    hits = index.search_hybrid("test dang nhap", k=3)
    assert all(0 < h.score < 1 for h in hits), "diem RRF luon nho va co bien"
    assert "vector=" in hits[0].why() and "tu_khoa=" in hits[0].why()


def test_alpha_doi_ket_qua___day_la_tham_so_phai_TINH_CHINH():
    index = VectorIndex(CORPUS)
    only_keyword = index.search_hybrid("#cancel-btn", k=1, alpha=0.0)
    only_vector = index.search_hybrid("#cancel-btn", k=1, alpha=1.0)
    assert only_keyword[0].chunk.chunk_id == "t2"
    # Khong assert only_vector ra gi: cai chinh la hai cau hinh cho ket qua KHAC nhau,
    # nen alpha phai duoc chon bang bo vang, khong phai chon bang cam tinh.
    assert only_vector[0].vector_score > 0


def test_chunk_theo_RANH_GIOI_THE_khong_xe_doi_selector():
    """Cat mu theo so ky tu se xe `<button id="sub` | `mit-btn">` -> selector bien mat."""
    html = "".join(f'<div id="row-{i}">noi dung hang so {i} trong bang ket qua</div>' for i in range(10))
    chunks = chunk_dom(html, max_chars=120, overlap=30)

    assert len(chunks) > 1
    rejoined = " ".join(c.text for c in chunks)
    for i in range(10):
        assert f'id="row-{i}"' in rejoined, f"selector row-{i} bi xe doi"


def test_chunk_rong_va_chunk_ngan_khong_lam_no():
    assert chunk_dom("") == []
    assert len(chunk_dom("<p>ngan</p>", max_chars=1000)) == 1


def test_recall_at_k_do_rieng_khau_TIM_khoi_khau_SINH():
    """recall@k = 0.4 nghia la 60% cau hoi khong bao gio co co hoi tra loi dung.

    Luc do toi uu prompt la vo ich — phai sua khau tim. Khong do rieng thi khong biet.
    """
    index = VectorIndex(CORPUS)
    hits = index.search_hybrid("test nao dung #cancel-btn", k=3)
    assert recall_at_k(hits, ["t2"], k=1) == 1.0
    assert recall_at_k(hits, ["t2", "t3"], k=1) == 0.5
    assert recall_at_k(hits, [], k=1) == 1.0  # khong co ca lien quan -> khong tru diem


def test_context_co_TRAN_va_co_GHI_NGUON():
    index = VectorIndex(CORPUS)
    context = build_context(index.search_hybrid("test dang nhap", k=5), max_chars=200)

    assert len(context) <= 260, "phai cat theo tran, khong nhoi het vao prompt"
    assert "nguon: test-cu#" in context, "khong ghi nguon thi khong kiem chung duoc"
    assert context.startswith("[1]")


def test_context_rong_khi_khong_tim_duoc_gi():
    assert build_context([]) == ""
    # Tra ve chuoi rong, KHONG nem. Khong tim duoc gi la ket qua hop le - luc do
    # agent phai noi "toi khong co du thong tin", chu khong phai sap ca phien.
