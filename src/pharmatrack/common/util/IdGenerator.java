package pharmatrack.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {
    private static final ConcurrentHashMap<String, AtomicInteger> SEQUENCES = new ConcurrentHashMap<>();

    private IdGenerator() {
    }

    public static String nextId(String prefix) {
        AtomicInteger seq = SEQUENCES.computeIfAbsent(prefix, p -> new AtomicInteger(0));
        int val = seq.incrementAndGet();
        return String.format("%s%03d", prefix, val);
    }

    public static void setSequenceMinimum(String prefix, int minVal) {
        SEQUENCES.compute(prefix, (k, existing) -> {
            if (existing == null) {
                return new AtomicInteger(minVal);
            }
            existing.updateAndGet(current -> Math.max(current, minVal));
            return existing;
        });
    }

    public static void reset() {
        SEQUENCES.clear();
    }
}
