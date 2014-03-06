package com.lunatech.labs.taskrunner

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BasicTaskRunnerTests extends TaskRunnerTests {

  trait Runnable {
    def run(): Unit
  }

  type T = Runnable

  implicit val runnableTask = new Task[Runnable] {
    override def run(runnable: Runnable) = Future { runnable.run() }
  }

  val actorSystem = ActorSystem("test")
  implicit val scheduler = actorSystem.scheduler

  override def getTask(fn: () => Unit): T = new Runnable { def run() = fn() }

  override def getTaskRunner(retryStrategy: RetryStrategy[T]): TaskRunner[T] =
    new BasicTaskRunner[T](retryStrategy)

}
