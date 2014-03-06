package com.lunatech.labs.taskrunner.persistent

import scala.concurrent.ExecutionContext.Implicits.global

class InMemoryTaskStoreTests extends TaskStoreTests {

  override type Task = String

  override def runWithStore[A](example: TaskStore[Task] => A): A = {
    val store = new InMemoryTaskStore[Task]
    example(store)
  }

  override def generateTask = "foo"
}
