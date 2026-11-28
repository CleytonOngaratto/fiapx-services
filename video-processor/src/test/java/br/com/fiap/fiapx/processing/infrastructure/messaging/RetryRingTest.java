package br.com.fiap.fiapx.processing.infrastructure.messaging;

import static br.com.fiap.fiapx.processing.infrastructure.messaging.MessagingTopology.DEAD_LETTER_EXCHANGE;
import static br.com.fiap.fiapx.processing.infrastructure.messaging.MessagingTopology.DEAD_LETTER_QUEUE;
import static br.com.fiap.fiapx.processing.infrastructure.messaging.MessagingTopology.SUBMITTED_KEY;
import static br.com.fiap.fiapx.processing.infrastructure.messaging.MessagingTopology.SUBMITTED_QUEUE;
import static br.com.fiap.fiapx.processing.infrastructure.messaging.MessagingTopology.WORK_EXCHANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

class RetryRingTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

    private static RabbitMQContainer broker;
    private static Connection connection;

    private Channel channel;

    @BeforeAll
    static void startBroker() throws Exception {
        broker = new RabbitMQContainer(
                DockerImageName.parse("rabbitmq:4-management-alpine")
                        .asCompatibleSubstituteFor("rabbitmq"));
        broker.start();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(broker.getAmqpUrl());
        connection = factory.newConnection();
    }

    @AfterAll
    static void stopBroker() throws Exception {
        if (connection != null) {
            connection.close();
        }
        broker.stop();
    }

    @BeforeEach
    void freshTopology() throws Exception {
        channel = connection.createChannel();
        for (String queue : List.of(SUBMITTED_QUEUE, MessagingTopology.RETRY_QUEUE, DEAD_LETTER_QUEUE)) {
            channel.queueDelete(queue);
        }
        MessagingTopology.declare(channel, RETRY_DELAY);
        channel.basicQos(1);
    }

    @Test
    void a_message_that_always_fails_is_attempted_max_times_and_then_lands_in_the_dlq()
            throws Exception {
        List<Long> observedCounts = new CopyOnWriteArrayList<>();
        CountDownLatch reachedDlq = new CountDownLatch(1);

        channel.basicConsume(SUBMITTED_QUEUE, false, (tag, delivery) -> {
            long rejected = DeliveryAttempts.rejectedCount(
                    delivery.getProperties().getHeaders(), SUBMITTED_QUEUE);
            observedCounts.add(rejected);

            if (DeliveryAttempts.exhausted(rejected, MAX_ATTEMPTS)) {
                channel.basicPublish(DEAD_LETTER_EXCHANGE, SUBMITTED_KEY,
                        delivery.getProperties(), delivery.getBody());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                reachedDlq.countDown();
            } else {
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            }
        }, tag -> {});

        publish("sempre-falha");

        assertTrue(reachedDlq.await(60, TimeUnit.SECONDS), "a mensagem nunca chegou na DLQ");

        assertEquals(List.of(0L, 1L, 2L, 3L), observedCounts,
                "a contagem de rejeicoes deve crescer de uma em uma a cada volta do anel");
        assertEquals(MAX_ATTEMPTS, observedCounts.size() - 1,
                "o numero de tentativas de processamento deve ser exatamente MAX_ATTEMPTS");

        GetResponse dead = channel.basicGet(DEAD_LETTER_QUEUE, true);
        assertEquals("sempre-falha", new String(dead.getBody(), StandardCharsets.UTF_8));
        assertEquals(0, channel.messageCount(SUBMITTED_QUEUE));
        assertEquals(0, channel.messageCount(MessagingTopology.RETRY_QUEUE));
    }

    @Test
    void a_message_that_succeeds_on_the_second_attempt_never_reaches_the_dlq() throws Exception {
        AtomicInteger deliveries = new AtomicInteger();
        CountDownLatch acked = new CountDownLatch(1);

        channel.basicConsume(SUBMITTED_QUEUE, false, (tag, delivery) -> {
            if (deliveries.incrementAndGet() == 1) {
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            } else {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                acked.countDown();
            }
        }, tag -> {});

        publish("falha-uma-vez");

        assertTrue(acked.await(60, TimeUnit.SECONDS), "a mensagem nunca foi confirmada");
        assertEquals(2, deliveries.get());
        assertEquals(0, channel.messageCount(DEAD_LETTER_QUEUE));
        assertEquals(0, channel.messageCount(SUBMITTED_QUEUE));
    }

    private void publish(String body) throws Exception {
        channel.basicPublish(WORK_EXCHANGE, SUBMITTED_KEY, null,
                body.getBytes(StandardCharsets.UTF_8));
    }
}
