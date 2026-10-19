package com.prep.onbai.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Gop request (request coalescing): N luong cung miss MOT key -> chi MOT luong goi loader, N-1 luong cho ket qua do.
 * Chong cache stampede khi key nong het han. Loi cung chia se cho moi nguoi dang cho, va KHONG bi cache lai.
 */
public final class SingleFlight<K, V> {

    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    public V load(K key, Function<K, V> loader) {
        CompletableFuture<V> mine = new CompletableFuture<>();
        CompletableFuture<V> running = inFlight.putIfAbsent(key, mine);
        if (running != null) {
            try {
                return running.join(); // nguoi khac dang nap: doi ket qua cua ho
            } catch (CompletionException e) {
                throw e.getCause() instanceof RuntimeException r ? r : e;
            }
        }
        try {
            V v = loader.apply(key);
            mine.complete(v);
            return v;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine); // xong thi go: lan miss sau lai nap moi
        }
    }
}
