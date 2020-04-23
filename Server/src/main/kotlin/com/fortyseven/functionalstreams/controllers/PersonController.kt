package com.fortyseven.functionalstreams.controllers

import com.fortyseven.functionalstreams.models.consumer.Person
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PersonController {

  companion object {
    private val personList: List<Person> = listOf(
      Person(1, "John"),
      Person(2, "Jane"),
      Person(3, "Max"),
      Person(4, "Alex"),
      Person(5, "Aloy"),
      Person(6, "Sarah")
    )
  }

  @GetMapping("/person/{id}")
  @Throws(InterruptedException::class)
  fun getPerson(@PathVariable id: Int, @RequestParam(defaultValue = "2") delay: Int): Person {
    Thread.sleep(delay * 1000.toLong()) // emulate network lag
    return personList.first { (id1) -> id1 == id }
  }
}
