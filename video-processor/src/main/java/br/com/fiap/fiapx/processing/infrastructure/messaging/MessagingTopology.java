package br.com.fiap.fiapx.processing.infrastructure.messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public final class MessagingTopology {

    public static final String WORK_EXCHANGE = "fiapx.videos";
    public static final String RETRY_EXCHANGE = "fiapx.videos.retry";
    public static final String DEAD_LETTER_EXCHANGE = "fiapx.videos.dlx";

    public static final String SUBMITTED_QUEUE = "video.submitted";
    public static final String RETRY_QUEUE = "video.submitted.retry";
    public static final String DEAD_LETTER_QUEUE = "video.submitted.dlq";

    public static final String SUBMITTED_KEY = "video.submitted";

    private MessagingTopology() {}

    public static void declare(Channel channel, Duration retryDelay) throws IOException {
        channel.exchangeDeclare(WORK_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.exchangeDeclare(RETRY_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.exchangeDeclare(DEAD_LETTER_EXCHANGE, BuiltinExchangeType.DIRECT, true);

        channel.queueDeclare(SUBMITTED_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", RETRY_EXCHANGE));
        channel.queueBind(SUBMITTED_QUEUE, WORK_EXCHANGE, SUBMITTED_KEY);

        // O anel: a mensagem rejeitada cai aqui, espera o TTL e é devolvida à fila principal
        // pelo próprio broker. Substitui o rabbitmq_delayed_message_exchange, que não vem na
        // imagem base.
        channel.queueDeclare(RETRY_QUEUE, true, false, false, Map.of(
                "x-dead-letter-exchange", WORK_EXCHANGE,
                "x-message-ttl", retryDelay.toMillis()));
        channel.queueBind(RETRY_QUEUE, RETRY_EXCHANGE, SUBMITTED_KEY);

        channel.queueDeclare(DEAD_LETTER_QUEUE, true, false, false, null);
        channel.queueBind(DEAD_LETTER_QUEUE, DEAD_LETTER_EXCHANGE, SUBMITTED_KEY);
    }
}
