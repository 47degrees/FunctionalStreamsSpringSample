package com.fortyseven.functionalstreams.consumer

import com.fortyseven.functionalstreams.consumer.models.Person
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors
import java.util.stream.Stream


object CallPersonUsingWebClient {

  private val logger = LoggerFactory.getLogger(CallPersonUsingWebClient::class.java)
  private const val baseUrl = "http://localhost:8080"
  private val client: WebClient = WebClient.create(baseUrl)

  @JvmStatic
  fun main(args: Array<String>) {
    val start = Instant.now()
    //for (i in 1..5) {
    //client.get().uri("/person/{id}", i).retrieve().bodyToMono(Person::class.java)
    // .subscribe()
    // Request is sent but .subscribe() doesn't wait for the response.
    // Since it doesn't block, it finished before receiving the response at all.
    //.block()
    // This would block til it receives each response, so defeats the purpose of reactive streams.
    //}

    /*
     We make a list of type Mono and wait for all of them to complete, rather than waiting
     for each one:
     */
    val list: List<Mono<Person>> = Stream.of(1, 2, 3, 4, 5)
      .map { i -> client.get().uri("/person/{id}", i).retrieve().bodyToMono(Person::class.java) }
      .collect(Collectors.toList())

    Mono.`when`(list).block()
    logTime(start)
  }

  private fun logTime(start: Instant) {
    logger.debug("Elapsed time: " + Duration.between(start, Instant.now()).toMillis() + "ms")
  }
}
