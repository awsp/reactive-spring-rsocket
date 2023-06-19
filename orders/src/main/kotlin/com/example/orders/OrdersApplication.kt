package com.example.orders

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Sample RSocket Server running at port: 8181
 */
@SpringBootApplication
class OrdersApplication

fun main(args: Array<String>) {
    runApplication<OrdersApplication>(*args)
}

/**
 * DTO - Order
 */
data class Order(
    var id: Int,
    var customerId: Int,
    var name: String,
)

/**
 * RSocket Controller
 */
@Controller
class OrderRSocketController {

    private val db = mutableMapOf<Int, Collection<Order>>()

    init {
        for (customerId in 0..3) {
            this.db[customerId] = randomOrdersFor(customerId);
        }
    }

    private fun randomOrdersFor(customerId: Int): Collection<Order> {
        val listOfOrders = mutableListOf<Order>()
        val max = (Math.random() * 1000).toInt();
        for (orderId in 1..(max)) {
            listOfOrders.add(Order(orderId, customerId, "order $orderId"))
        }
        return listOfOrders;
    }

    /**
     * RSocket Endpoint
     * Use rsc to query. for Mac M1, download the jar instead.
     *
     * Sample x86:
     *   rsc tcp://localhost:8181 -r orders.2 --stream
     *
     * Sample m1:
     *   java -jar rsc.jar tcp://localhost:8181 -r orders.2 --stream
     */
    @MessageMapping("orders.{customerId}")
    fun getOrdersFor(@DestinationVariable customerId: Int) =
        Flux.fromIterable(this.db[customerId]!!.toList())

    /**
     * Fire and forget model
     *
     * CLI:
     * rsc tcp://localhost:8181 -r fire-and-forget -d '{"name": "abc", "id": 1}'
     */
    @MessageMapping("fire-and-forget")
    fun fireAndForget(request: Mono<Order>): Mono<Order> {
        return request
            .doOnNext { t -> t.name = t.name + " (Updated)" }
            .doOnNext { t -> println(t.name) }
    }

    /**
     * Request and Response model
     *
     * CLI:
     * rsc tcp://localhost:8181 -r request-response -d '{"name": "abc", "id": 1}' --request
     */
    @MessageMapping("request-response")
    fun requestResponse(request: Mono<Order>): Mono<Order> {
        return request
            .doOnNext { t -> t.name = t.name + " (Updated)" }
            .doOnNext { t -> println("RR: " + t.name) }
    }

    @MessageMapping("request-response-stream")
    fun requestResponseStream(request: Mono<Order>): Flux<Order> {
        // TODO: use index 0 for now
        return Flux.fromIterable(this.db[0]!!.toList())
    }

    @MessageMapping("bi-directional-stream")
    fun biDirectionalStream(request: Flux<Order>): Flux<Order> {
        // TODO: use index 0 for now
        return Flux.fromIterable(this.db[0]!!.toList())
    }
}
