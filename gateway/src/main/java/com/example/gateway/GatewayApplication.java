package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Notes - Benefits of a gateway:
 * - With a minimal of intrusion introduces concern applicable to downstream services
 * - No core business logic or edge services
 * - Good for generic things like compression, routing, load balancing, rate limiting
 * - Centralized security behind gateway
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    /**
     * Somewhat client, either side can request or response
     * @param builder RSocketRequester.Builder
     * @return RSocketRequester
     */
    @Bean
    RSocketRequester rSocket(RSocketRequester.Builder builder) {
        return builder.tcp("localhost", 8181);
    }

    /**
     * For non-blocking http request
     * @param builder WebClient.Builder
     * @return WebClient
     */
    @Bean
    WebClient http(WebClient.Builder builder) {
        return builder.build();
    }


    /**
     * API Gateway
     * Notes:
     * Route has YAML configuration can live spring cloud config server. refresh dynamically
     * ---
     * Captured by *.spring.io
     * curl -H"Host: test.spring.io" http://localhost:8888/proxy
     *
     * @param builder RouteLocatorBuilder
     * @return RouteLocator
     */
    @Bean
    RouteLocator gateway(RouteLocatorBuilder builder) {
        return builder
                .routes()
                // Can have more than 1 route / path
                .route(rs -> rs
                        .path("/proxy").and().host("*.spring.io")
                        // Change downstream path, circuit breaker, retry, rate limiting, refresh, query string
                        // In thd following example, only path change
                        .filters(fs -> fs.setPath("/customers"))
                        .uri("http://localhost:8012"))
                .build();
    }
}

@RestController
@RequiredArgsConstructor
class CustomerOrdersRestController {
    private final CrmClient crmClient;

    /**
     * Endpoint /cos
     * ---
     * Sample:
     * curl http://localhost:8888/cos
     *
     * @return Flux
     */
    @GetMapping("/cos")
    Flux<CustomerOrders> get() {
        return crmClient.getCustomerOrders();
    }
}


/**
 * API Adapter
 * Calls both endpoint to compose composite view
 * --
 * Notes:
 * - Aggregate data into a composite view.
 */
@Component
@RequiredArgsConstructor
class CrmClient {
    private final RSocketRequester rSocket;
    private final WebClient http;

    Flux<Customer> getCustomers() {
        return http.get().uri("http://localhost:8012/customers").retrieve().bodyToFlux(Customer.class)
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(10)))
                .onErrorResume(ex -> Flux.empty())
                .timeout(Duration.ofSeconds(10));
    }

    Flux<Order> getOrdersFor(Integer customerId) {
        return rSocket.route("orders.{cid}", customerId).retrieveFlux(Order.class)
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(10)))
                .onErrorResume(ex -> Flux.empty())
                .timeout(Duration.ofSeconds(10));
    }

    Flux<CustomerOrders> getCustomerOrders() {
        return getCustomers()
                .flatMap(customer -> Mono.zip(
                        Mono.just(customer),
                        getOrdersFor(customer.getId()).collectList()))
                .map(tuple -> new CustomerOrders(tuple.getT1(), tuple.getT2()));
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerOrders {
    private Customer customer;
    private List<Order> orders;
}

@Data
@NoArgsConstructor
class Order {
    private Integer id, customerId;
}

@Data
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}