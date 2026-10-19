package com.prep.cs.counting;

import static com.prep.cs.counting.ClientWindow.Verdict.DUPLICATE;
import static com.prep.cs.counting.ClientWindow.Verdict.NEW;
import static com.prep.cs.counting.ClientWindow.Verdict.STALE;
import static com.prep.cs.counting.ClientWindow.Verdict.TOO_OLD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClientWindowTest {

    @Test
    void retryIsDuplicateLateButUnseenIsNew() {
        ClientWindow w = new ClientWindow(1, 10, 0);       // lan dau thay seq 10
        assertThat(w.accept(1, 11, 0)).isEqualTo(NEW);
        assertThat(w.accept(1, 11, 0)).isEqualTo(DUPLICATE); // client retry seq 11
        assertThat(w.accept(1, 14, 0)).isEqualTo(NEW);       // 12, 13 chua toi (dang tren duong)
        assertThat(w.accept(1, 12, 0)).isEqualTo(NEW);       // toi tre nhung chua thay -> van dem
        assertThat(w.accept(1, 13, 0)).isEqualTo(NEW);
        assertThat(w.accept(1, 12, 0)).isEqualTo(DUPLICATE);
        assertThat(w.accept(1, 10, 0)).isEqualTo(DUPLICATE);
    }

    @Test
    void windowIs64AndEpochResets() {
        ClientWindow w = new ClientWindow(1, 0, 0);
        assertThat(w.accept(1, 100, 0)).isEqualTo(NEW);
        assertThat(w.accept(1, 37, 0)).isEqualTo(NEW);       // 100 - 37 = 63 < 64: con trong cua so
        assertThat(w.accept(1, 36, 0)).isEqualTo(TOO_OLD);   // 64 so ve truoc: khong con biet -> bo, dem rieng
        assertThat(w.accept(2, 0, 0)).isEqualTo(NEW);        // client khoi dong lai: epoch 2, seq tu 0
        assertThat(w.accept(2, 0, 0)).isEqualTo(DUPLICATE);
        assertThat(w.accept(1, 101, 0)).isEqualTo(STALE);    // tien trinh cu (epoch 1) van con gui: xac song
    }
}
