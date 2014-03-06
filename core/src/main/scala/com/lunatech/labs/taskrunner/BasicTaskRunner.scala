package com.lunatech.labs.taskrunner

import akka.actor.Scheduler
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.FiniteDuration

/**
 * Basic task runner. Not persistent.
 *
 * Used an Akka scheduler to schedule retries.
 */
class BasicTaskRunner[A: Task](retryStrategy: RetryStrategy[A], onFatalException: Throwable ⇒ Any = _ ⇒ ())(implicit scheduler: Scheduler, ec: ExecutionContext) extends TaskRunner[A] {

  override def runTask(task: A, delay: Option[FiniteDuration]): Future[Unit] = {
    delay match {
      case None => execute(task, 0)
      case Some(delay) => scheduleTask(task, delay, 0)
    }
    Future.successful(())
  }

  private def execute(task: A, retries: Int): Unit =
    implicitly[Task[A]].run(task) onFailure { throwable ⇒
      retryStrategy.nextRetryDelay(task, retries, throwable) match {
        case Some(delay) ⇒ scheduleTask(task, delay, retries + 1)
        case None ⇒ onFatalException(throwable)
      }
    }

  private def scheduleTask(task: A, delay: FiniteDuration, retries: Int): Unit = {
    scheduler.scheduleOnce(delay) { execute(task, retries) }
    ()
  }
}
