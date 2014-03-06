package com.lunatech.labs.taskrunner.persistent

import akka.actor.ActorSystem
import com.lunatech.labs.taskrunner.{ RetryStrategy, Task, TaskRunner, TaskRunnerTests }
import com.lunatech.labs.taskrunner.retrystrategies.NoRetries
import org.mockito.{ AdditionalMatchers, Matchers }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import java.sql.Timestamp
import java.util.Date

class PersistentTaskRunnerTests extends TaskRunnerTests {

  trait Runnable {
    def run(): Unit
  }

  type T = Runnable

  implicit val runnableTask = new Task[Runnable] {
    override def run(runnable: Runnable) = Future { runnable.run() }
  }

  val actorSystem = ActorSystem("test")
  implicit val scheduler = actorSystem.scheduler

  override def getTask(fn: () ⇒ Unit): T = new Runnable { def run() = fn() }

  override def getTaskRunner(retryStrategy: RetryStrategy[T]): TaskRunner[T] = {
    val taskStore = new InMemoryTaskStore[T]
    new PersistentTaskRunner[T](taskStore, retryStrategy)
  }

  "The persistent taskrunner" should {
    "not run a task if it didn't successfully register at the taskstore" in {
      val taskStore = new InMemoryTaskStore[T] {
        override def register(task: T, runAt: Option[Timestamp] = None) = Future.failed(new RuntimeException("Foo!"))
      }
      val taskRunner = new PersistentTaskRunner(taskStore, NoRetries)

      var ran = false
      val task = getTask(() ⇒ {
        ran = true
      })

      taskRunner.runTask(task)

      Thread.sleep(500)
      ran must_== false
    }

    "mark a task as failed with exception and nextTry, if it temporarily failed" in {
      val taskStore = spy(new InMemoryTaskStore[T])
      val exception = new RuntimeException("Foo!")
      val strategy = mock[RetryStrategy[Any]]
      val task = getTask(() ⇒ throw exception)

      strategy.nextRetryDelay(any, Matchers.eq(0), Matchers.eq(exception)) returns Some(Duration(0, "seconds"))
      strategy.nextRetryDelay(any, Matchers.eq(1), Matchers.eq(exception)) returns None

      val taskRunner = new PersistentTaskRunner(taskStore, strategy)

      taskRunner.runTask(task)

      there was one(taskStore).markFailed(any, Matchers.eq(exception), AdditionalMatchers.not(Matchers.eq(None)))
    }

    "mark a task as failed with exception and no nextTry, if it fatally failed" in {
      val taskStore = spy(new InMemoryTaskStore[T])
      val taskRunner = new PersistentTaskRunner(taskStore, NoRetries)
      val exception = new RuntimeException("Foo!")
      val task = getTask(() ⇒ throw exception)

      taskRunner.runTask(task)

      there was one(taskStore).markFailed(any, Matchers.eq(exception), Matchers.eq(None))
    }

    "unregister a task on success" in {
      val taskStore = spy(new InMemoryTaskStore[T])

      val taskRunner = new PersistentTaskRunner(taskStore, NoRetries)

      val task = getTask(() ⇒ ())

      taskRunner.runTask(task)

      there was one(taskStore).unregister(any)
    }

    "not unregister a task on failure" in {
      val taskStore = spy(new InMemoryTaskStore[T])

      val taskRunner = new PersistentTaskRunner(taskStore, NoRetries)

      val task = getTask(() ⇒ throw new RuntimeException("Foo!"))
      taskRunner.runTask(task)

      Thread.sleep(500)

      there was no(taskStore).unregister(any)
    }

    "succesfully restores and runs scheduled tasks" in {
      val taskStore = new InMemoryTaskStore[T]

      var completed = false
      val task = getTask(() => completed = true)

      val taskId = Await.result(taskStore.register(task), Duration(1, "seconds"))
      taskStore.markFailed(taskId, new RuntimeException("Foo!"), Some(new Timestamp((new Date).getTime + 300)))

      val taskRunner = new PersistentTaskRunner(taskStore, NoRetries)

      completed must beTrue.eventually

    }

    "doesn't restore and run tasks that are failed and not rescheduled" in {
      val taskStore = new InMemoryTaskStore[T]

      var completed = false
      val task = getTask(() => completed = true)

      val taskId = Await.result(taskStore.register(task), Duration(1, "seconds"))
      taskStore.markFailed(taskId, new RuntimeException("Foo!"), None)

      val taskRunner = new PersistentTaskRunner(taskStore, NoRetries)

      Thread.sleep(500)
      completed must_== false
    }

    "asks the retry manager whether a task that's scheduled but overdue must be rescheduled" in {
      val taskStore = new InMemoryTaskStore[T]

      var completed = false
      val task = getTask(() => completed = true)

      val taskId = Await.result(taskStore.register(task), Duration(1, "seconds"))
      val scheduledTime = new Timestamp((new Date).getTime - 300)
      taskStore.markFailed(taskId, new RuntimeException("Foo!"), Some(scheduledTime))

      val retryStrategy = mock[RetryStrategy[T]]
      retryStrategy.nextRetryDelay(any, any, any) returns None
      val taskRunner = new PersistentTaskRunner(taskStore, retryStrategy)

      there was one(retryStrategy).nextRetryDelay(any, Matchers.eq(0), Matchers.eq(PersistentTaskRunner.ScheduleMissedException(scheduledTime)))

      Thread.sleep(500)
      completed must_== false
    }

    "asks the retry strategy whether a task that's interrupted while busy must be rescheduled" in {
      val taskStore = new InMemoryTaskStore[T]

      var completed = false
      val task = getTask(() => completed = true)

      val taskId = Await.result(taskStore.register(task), Duration(1, "seconds"))
      val scheduledTime = new Timestamp((new Date).getTime - 300)

      val retryStrategy = mock[RetryStrategy[T]]
      retryStrategy.nextRetryDelay(any, any, any) returns None
      val taskRunner = new PersistentTaskRunner(taskStore, retryStrategy)

      there was one(retryStrategy).nextRetryDelay(any, Matchers.eq(0), Matchers.eq(PersistentTaskRunner.TaskInterruptedException))

      Thread.sleep(500)
      completed must_== false
    }
  }
}

