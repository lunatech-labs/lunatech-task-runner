package com.lunatech.labs.taskrunner

import akka.actor.ActorSystem
import org.specs2.matcher.TerminationMatchers
import org.specs2.mock.{ Mockito ⇒ SpecsMockito }
import org.mockito.Mockito._
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.lunatech.labs.taskrunner.retrystrategies.NoRetries
import org.mockito.Matchers
import java.util.Date

abstract class TaskRunnerTests extends Specification with TerminationMatchers with SpecsMockito {

  // The 'task' type for this task runner
  type T

  def getTaskRunner(retryStrategy: RetryStrategy[T]): TaskRunner[T]
  def getTask(fn: () ⇒ Unit): T

  "The task registry" should {

    "immediately return when the task is accepted, and not wait until the task is executed" in {
      val taskRunner = getTaskRunner(NoRetries)
      val slowTask = getTask(() ⇒ Thread.sleep(1000))

      taskRunner.runTask(slowTask) must terminate(retries = 5, sleep = 100.millis)
    }

    "run the task that's passed to it" in {
      val taskRunner = getTaskRunner(NoRetries)
      var ran = false
      val task = getTask(() ⇒ ran = true)

      taskRunner.runTask(task)

      ran must beTrue.eventually
    }

    "run the task with the proper delay" in {
      val taskRunner = getTaskRunner(NoRetries)
      var ran = false
      val task = getTask(() ⇒ ran = true)

      taskRunner.runTask(task, Some(Duration(500, "milliseconds")))
      Thread.sleep(300)
      ran must_== false

      ran must beTrue.eventually

    }

    "not look at the RetryStrategy if the task succeeds the first time" in {
      val strategy = mock[RetryStrategy[T]]
      val taskRunner = getTaskRunner(strategy)
      val task = getTask(() ⇒ ())

      taskRunner.runTask(task)

      there was no(strategy).nextRetryDelay(any, anyInt, any[Throwable])
    }

    "invoke the 'nextRetryDelay' method on the RetryStrategy with the thrown exception and correct number of retries if the task fails" in {
      val strategy = mock[RetryStrategy[T]]
      val taskRunner = getTaskRunner(strategy)
      val exception = new RuntimeException("Foo!")
      val task = getTask(() ⇒ throw exception)
      strategy.nextRetryDelay(task, 0, exception) returns Some(Duration.Zero)
      strategy.nextRetryDelay(task, 1, exception) returns None

      taskRunner.runTask(task)

      Thread.sleep(1000)

      there was one(strategy).nextRetryDelay(task, 0, exception)
      there was one(strategy).nextRetryDelay(task, 1, exception)
      there was no(strategy).nextRetryDelay(task, 2, exception)
    }

    "not retry the task if the RetryStrategy says not to" in {
      val taskRunner = getTaskRunner(NoRetries)
      var ran = 0
      val task = getTask(() ⇒ {
        ran = ran + 1
        throw new RuntimeException("Foo!")
      })

      taskRunner.runTask(task)

      ran must be_==(1).eventually
    }

    "retry the task if the RetryStrategy says to, until the RetryStrategy says not to anymore" in {
      val strategy = mock[RetryStrategy[T]]
      val taskRunner = getTaskRunner(strategy)
      val exception = new RuntimeException("Foo!")
      var ran = 0
      val task = getTask(() ⇒ {
        ran = ran + 1
        throw exception
      })

      strategy.nextRetryDelay(any, Matchers.eq(0), Matchers.eq(exception)) returns Some(Duration(0, "seconds"))
      strategy.nextRetryDelay(any, Matchers.eq(1), Matchers.eq(exception)) returns Some(Duration(0, "seconds"))
      strategy.nextRetryDelay(any, Matchers.eq(2), Matchers.eq(exception)) returns None

      taskRunner.runTask(task)

      ran must be_==(3).eventually

    }

    "retry a task after the nextRetryDelay specified by the RetryStrategy" in {
      val strategy = mock[RetryStrategy[T]]
      val taskRunner = getTaskRunner(strategy)
      val exception = new RuntimeException("Foo!")
      var ran = 0
      val task = getTask(() ⇒ {
        ran = ran + 1
        throw exception
      })

      strategy.nextRetryDelay(any, Matchers.eq(0), Matchers.eq(exception)) returns Some(Duration(1, "seconds"))
      strategy.nextRetryDelay(any, Matchers.eq(1), Matchers.eq(exception)) returns None

      taskRunner.runTask(task)

      // Should not be executed a second time after half a second
      Thread.sleep(500)
      ran must_== 1

      // Should be executed a second time after (in total) one and a half second
      Thread.sleep(1000)
      ran must_== 2
    }

  }

}
