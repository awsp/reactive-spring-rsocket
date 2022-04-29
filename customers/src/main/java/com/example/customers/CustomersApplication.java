package com.example.customers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Sample Spring Reactive Web at port 8012
 * ---
 * Reactive Stream Specifications
 * - Publisher - Publishes a list of item of type T
 * - Subscriber - Consume the item in the onNext() method if it is available, onError() when failed,
 * onCompleted() that completes non exceptionally
 * - Subscription - bridge between Producer and the Consumer. => Flow control == Backpressure
 * - Producer
 * - Consumer
 * <p>
 * ---
 * Mono - A type of publisher that produces 1 item and supports backpressure
 * Flux - A type of publisher similar to Mono but produces N item with backpressure
 * Sample code:
 * Flux<String> items = Flux.just("AA", "BB", "CC", "DD")
 * Flux<Foo> fooItems = items.map(item -> new Foo(null, item));
 * <p>
 * Flux<Foo> saved = fooItems.flatMap(fooItem -> this.db.save(fooItem);
 * saved.subscribe(); // This will make the above cold statement hot!
 * ---
 * flatMap() vs map()
 * ---
 * flapMap() returns Mono, map() returns Flux
 * fooItems.map() will return Publisher<Publisher<Reservation>>
 * fooItems.flatMap() will return Flux<Reservation> instead.
 * Therefore,
 * Flux<Reservation> == Publisher<Publisher<Reservation>>
 */
@SpringBootApplication
public class CustomersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomersApplication.class, args);
    }

    /**
     * Application runner to init database schema
     *
     * @param dbc        DatabaseClient
     * @param repository CustomerRepository
     * @return ApplicationListener
     */
    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(DatabaseClient dbc,
                                                     CustomerRepository repository) {
        return event -> {
            var ddl = dbc.sql("create table if not exists customer(id serial primary key, name varchar(255) not null)")
                    .fetch()
                    .rowsUpdated();
            var saved = Flux.just("a", "b", "c")
                    .map(name -> new Customer(null, name))
                    .flatMap(repository::save);

            // TODO: Reactive subscribe
            ddl.thenMany(saved).subscribe(System.out::println);
        };
    }

    /**
     * Another type to define routes
     * <p>
     * http://localhost:8012/hello-from-router
     * http://localhost:8012/hello-from-router?name=awsp
     *
     * @return RouterFunction
     */
    @Bean
    RouterFunction<ServerResponse> router() {
        return RouterFunctions
                .route()
                .GET("/hello-from-router", request -> ServerResponse
                        .ok()
                        .body(Flux.just("Hello from router: " + request.queryParam("name")
                                .orElse("nobody")), String.class))
                .build();
    }
}


@Component
class IntervalMessagePublisher {
    Flux<GreetingsResponse> greetings(GreetingsRequest request) {
        return Flux
                .fromStream(Stream.generate(() -> "Hello " + request.getName() + " @ " + Instant.now()))
                .map(GreetingsResponse::new)
                .delayElements(Duration.ofSeconds(1));
    }
}

@RestController
@RequiredArgsConstructor
class CustomerRestController {

    private final CustomerRepository customerRepository;

    /**
     * Reactive Web endpoint /customers
     * Sample:
     * curl http://localhost:8012/customers
     *
     * @return Flux
     */
    @GetMapping("/customers")
    Flux<Customer> get() {
        return this.customerRepository.findAll();
    }
}

/**
 * In WebFlux, EVERYTHING is a publisher!
 * It's possible to have SSE and even WebSocket integrated
 */
@RestController
@RequiredArgsConstructor
class ServerSideMessagingRestController {
    private final IntervalMessagePublisher messagePublisher;

    /**
     * cURL: http://localhost:8012/sse/nobody
     *
     * @param name name
     * @return Publisher
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/sse/{name}")
    Publisher<GreetingsResponse> serverSideEvent(@PathVariable String name) {
        return messagePublisher.greetings(new GreetingsRequest(name));
    }
}


interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {

}

/**
 * DTO - Customer
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingsRequest {
    private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingsResponse {
    private String message;
}