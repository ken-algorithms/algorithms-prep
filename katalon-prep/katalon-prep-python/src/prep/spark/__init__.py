"""CHUA LAM — ban da quyet dinh de Spark sau, uu tien module `agent/` truoc.

Phan da xac minh duoc trong buoi nay, ghi lai de luc quay lai khong mat cong:

  * `uv sync --extra spark` -> PySpark 3.5.9 chay duoc. venv thanh **368 MB**.
    Xoa sach: `rm -rf .venv` (khong dong gi vao he thong, khong Docker).

  * **Spark 3.5 VO tren JDK 25** — day la thu se lam mat 30 phut neu khong biet truoc:

        py4j.protocol.Py4JJavaError: ...
        java.lang.UnsupportedOperationException: getSubject is not supported

    Nguyen nhan: JEP 486 (JDK 24+) go bo Security Manager, ma Spark 3.5 con goi
    `Subject.getSubject`. May ban mac dinh JDK 25 -> phai ghim:

        export JAVA_HOME=~/.sdkman/candidates/java/21.0.12-tem

    Chinh la JDK 21 da cai cho module `02`/`03` ben Java. Doi ung Spark 4.x thi ho
    tro JDK 21 chinh thuc, nhung 3.5 la ban Katalon co kha nang dang dung.

Noi dung du kien khi lam (theo dung thu tu gia tri cho phong van):

  1. **Sessionization** bang window function (`lag` -> co phien moi -> `sum` tich luy).
     Day CHINH LA loi cua TrueTest: gom event thanh journey. Lam xong bai nay la noi
     duoc khau `model` trong chuoi discover -> model -> generate -> maintain.

  2. **Data skew + salting** tren du lieu that. Noi thang tu
     `prep/distributed/resilience.py`: "hot partition" cua Kafka va "data skew" cua
     Spark la CUNG mot van de. Va con so da do duoc o day van dung: phai salt >= 16x
     so partition, salt 1x gan nhu vo dung.

  3. **Broadcast join vs shuffle join** — nguong `autoBroadcastJoinThreshold`.

  4. **`explain()`** — doc physical plan. Doi xung chinh xac voi `EXPLAIN (ANALYZE,
     BUFFERS)` o module 05 ben Java: cung mot ky nang, hai he thong.

  5. `repartition` vs `coalesce`; vi sao `collect()` giet driver; UDF Python ton chi
     phi serialize so voi ham built-in.
"""
