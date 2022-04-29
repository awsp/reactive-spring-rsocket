package com.example.messaging;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

/**
 * Java side WebSocket client
 */
public class ReactiveJavaClientWebSocket {

    public static void main(String[] args) {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        client.execute(URI.create("ws://localhost:8012/event-emitter"), session ->
                session.send(Mono.just(session.textMessage("hello from ReactiveJavaClientWebSocket")))
                        .thenMany(session.receive().map(WebSocketMessage::getPayloadAsText).log())
                        .then()).block(Duration.ofSeconds(10L));
    }
}
