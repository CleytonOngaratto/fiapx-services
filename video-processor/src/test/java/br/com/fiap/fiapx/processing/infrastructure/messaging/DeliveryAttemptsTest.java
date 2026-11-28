package br.com.fiap.fiapx.processing.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeliveryAttemptsTest {

    @Test
    void returns_zero_when_there_is_no_history() {
        assertEquals(0, DeliveryAttempts.rejectedCount(null, "q"));
        assertEquals(0, DeliveryAttempts.rejectedCount(Map.of(), "q"));
        assertEquals(0, DeliveryAttempts.rejectedCount(Map.of("x-death", "nao-e-lista"), "q"));
    }

    @Test
    void reads_the_count_of_the_rejected_entry_of_the_given_queue() {
        Map<String, Object> headers = Map.of("x-death", List.of(
                Map.of("reason", "expired", "queue", "video.submitted.retry", "count", 7L),
                Map.of("reason", "rejected", "queue", "video.submitted", "count", 2L)));

        assertEquals(2, DeliveryAttempts.rejectedCount(headers, "video.submitted"));
    }

    @Test
    void ignores_the_rejected_entry_of_another_queue() {
        Map<String, Object> headers = Map.of("x-death", List.of(
                Map.of("reason", "rejected", "queue", "outra.fila", "count", 9L)));

        assertEquals(0, DeliveryAttempts.rejectedCount(headers, "video.submitted"));
    }

    @Test
    void exhausted_only_when_the_count_reaches_the_limit() {
        assertFalse(DeliveryAttempts.exhausted(0, 3));
        assertFalse(DeliveryAttempts.exhausted(2, 3));
        assertTrue(DeliveryAttempts.exhausted(3, 3));
        assertTrue(DeliveryAttempts.exhausted(4, 3));
    }
}
