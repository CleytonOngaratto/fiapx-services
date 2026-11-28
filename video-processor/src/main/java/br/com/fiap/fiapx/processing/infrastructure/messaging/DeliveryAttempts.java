package br.com.fiap.fiapx.processing.infrastructure.messaging;

import java.util.List;
import java.util.Map;

public final class DeliveryAttempts {

    private DeliveryAttempts() {}

    public static long rejectedCount(Map<String, Object> headers, String queue) {
        if (headers == null) {
            return 0;
        }
        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List<?> entries)) {
            return 0;
        }
        for (Object entry : entries) {
            if (entry instanceof Map<?, ?> death
                    && "rejected".equals(text(death.get("reason")))
                    && queue.equals(text(death.get("queue")))
                    && death.get("count") instanceof Number count) {
                return count.longValue();
            }
        }
        return 0;
    }

    public static boolean exhausted(long rejectedCount, int maxAttempts) {
        return rejectedCount >= maxAttempts;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
