package com.prep.cs.counting;

import java.util.ArrayList;
import java.util.List;

/**
 * Tach khoang [from, to) (giay epoch) thanh it bucket nhat tren 4 tang. Tham lam tu trai sang: tai vi tri x,
 * lay tang LON NHAT ma x can dung bien cua no va bucket do nam tron trong khoang. Cac bucket lien tiep cung tang
 * gop thanh mot Range de moi tang chi la mot lan quet index.
 *
 *   02:59:58 -> 05:00:03  =  giay [02:59:58, 03:00:00) + gio [03:00, 05:00) + giay [05:00:00, 05:00:03)
 *
 * So bucket toi da: 59 + 59 + 23 (mep trai) + so ngay + 23 + 59 + 59 (mep phai) = 282 + so ngay,
 * KHONG phu thuoc so event. Do la ly do doc "24 gio qua" hay "ca nam" deu re.
 */
public final class RangeDecomposer {

    public record Range(Tier tier, long fromBucket, long toBucket) {      // toBucket: khong tinh
        public long buckets() {
            return (toBucket - fromBucket) / tier.seconds;
        }
    }

    private RangeDecomposer() {}

    public static List<Range> decompose(long from, long to) {
        List<Range> out = new ArrayList<>();
        long x = from;
        while (x < to) {
            Tier pick = Tier.SECOND;
            for (Tier t : Tier.COARSE_FIRST) {
                if (Math.floorMod(x, t.seconds) == 0 && x + t.seconds <= to) {
                    pick = t;
                    break;
                }
            }
            Range last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last.tier() == pick && last.toBucket() == x) {
                out.set(out.size() - 1, new Range(pick, last.fromBucket(), x + pick.seconds));
            } else {
                out.add(new Range(pick, x, x + pick.seconds));
            }
            x += pick.seconds;
        }
        return out;
    }

    public static long totalBuckets(List<Range> ranges) {
        return ranges.stream().mapToLong(Range::buckets).sum();
    }
}
