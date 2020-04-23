package com.fortyseven.functionalstreams.controllers

import com.fortyseven.functionalstreams.models.producer.Greeting
import org.reactivestreams.Publisher
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream


@RestController
class GreetReactiveController {

  @GetMapping("/greetings")
    /**
     * When we return Flux, the whole internal dynamics get changed compared to how it'd work with a blocking approach
     * (i.e: returning a List instead). The framework starts subscribing to these records from the publisher and it
     * serializes each item and sends it back to the client in chunks.
     *
     * URL: http://localhost:8080/greetings/
     */
  fun greetingPublisher(): Publisher<Greeting?> {

    // Flux.generate creates never ending stream of Greetings, we'll just take the first 50 emitted values.
    return Flux.generate { sink: SynchronousSink<Greeting> ->
      sink.next(Greeting("Hello"))
    }.take(50)
  }

  /**
   * Example of Server-Sent Events
   *
   * These events allow a web page to get updates from a server in real-time.
   * This method produces a TEXT_EVENT_STREAM_VALUE which essentially means that the data is being sent in the form of
   * Server-Sent events.
   *
   * URL: http://localhost:8080/greetings/sse
   */
  @GetMapping(value = ["/greetings/sse"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun events(): Flux<Greeting> {
    val greetingFlux: Flux<Greeting> = Flux.fromStream(
      Stream.generate { Greeting("Hello @" + Instant.now().toString()) }
    )
    val durationFlux = Flux.interval(Duration.ofSeconds(1))

    // generate values each second
    return Flux.zip(greetingFlux, durationFlux).map { tuple2 -> tuple2.t1 }

    /*
    Could also use delayElements(), like:

    val delayElements: Flux<Greeting> = Flux.generate { sink: SynchronousSink<Greeting> ->
    sink.next(Greeting("Hello @" + Instant.now().toString()))
    }.delayElements(Duration.ofSeconds(1))
     */
  }
}
