package com.lunatech.labs.taskrunner.persistent

import akka.actor.Scheduler
import com.lunatech.labs.taskrunner.{ RetryStrategy, Task, TaskRunner }
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.{ Failure, Success }

/**
 * A task runner that persists tasks in a `TaskStore`.
 *
 * @tparam A The type of task this Taskrunner can execute
 * @param taskStore The taskstore in which tasks are persisted
 * @param retryStrategy Strategy for retrying failed tasks
 * @param onFatalException Error handler that will be invoked when a task fails fatally; i.e. won't be retried anymore
 * @param onStoreException Error handler that will be invoked when there's an exception from the task store. A task affected by such an error won't be retried anymore.
 */
class PersistentTaskRunner[A: Task](taskStore: TaskStore[A], retryStrategy: RetryStrategy[A], onFatalException: Throwable ⇒ Any = _ ⇒ (), onStoreException: PersistentTaskRunner.StoreException ⇒ Any = _ ⇒ ())(implicit scheduler: Scheduler, ec: ExecutionContext) extends TaskRunner[A] {

  restoreTasks()

  override def runTask(task: A, delay: Option[FiniteDuration]): Future[Unit] = delay match {
    case None => taskStore.register(task).map { taskId ⇒
      execute(task, taskId, 0)
    }
    case Some(delay) if delay.toNanos == 0 => runTask(task, None)
    case Some(delay) => {
      val runAt = new Timestamp((new Date).getTime() + delay.toMillis)
      taskStore.register(task, Some(runAt)).map { taskId ⇒
        scheduleTask(task, taskId, delay, 0)
      }
    }
  }

  private def execute(task: A, taskId: taskStore.Id, tried: Int): Unit =
    implicitly[Task[A]].run(task) onComplete {
      case Success(_) ⇒
        taskStore.unregister(taskId).onFailure {
          case exception ⇒ onStoreException(PersistentTaskRunner.UnregisterException(taskId, exception))
        }
      case Failure(exception) ⇒ handleTaskException(task, taskId: taskStore.Id, tried, exception)
    }

  private def handleTaskException(task: A, taskId: taskStore.Id, tried: Int, exception: Throwable) = {
    retryStrategy.nextRetryDelay(task, tried + 1, exception) match {
      case Some(delay) ⇒ {
        taskStore.markFailed(taskId, exception, Some(timestampAfter(delay))) onComplete {
          case Success(_) ⇒ scheduleTask(task, taskId, delay, tried + 1)
          case Failure(ex) ⇒ onStoreException(PersistentTaskRunner.MarkFailedException(taskId, exception))
        }
      }
      case None ⇒ {
        taskStore.markFailed(taskId, exception, None) onFailure {
          case ex ⇒ onStoreException(PersistentTaskRunner.MarkFailedException(taskId, ex))
        }
        onFatalException(exception)
      }
    }
  }

  private def scheduleTask(task: A, taskId: taskStore.Id, delay: FiniteDuration, tried: Int): Unit = {
    scheduler.scheduleOnce(delay) {
      taskStore.markBusy(taskId).onComplete {
        case Success(_) ⇒ execute(task, taskId, tried)
        case Failure(exception) ⇒ onStoreException(PersistentTaskRunner.MarkBusyException(taskId, exception))
      }
    }
    ()
  }

  private def timestampAfter(delay: FiniteDuration): Timestamp =
    new Timestamp((new Date).getTime + delay.toMillis)

  private def restoreTasks() = {
    val busy = Await.result(taskStore.listBusy, Duration(1, "minute"))
    val retryable = Await.result(taskStore.listRetryable, Duration(1, "minute"))

    busy.foreach { registeredTask =>
      handleTaskException(registeredTask.task, registeredTask.id, registeredTask.tried, PersistentTaskRunner.TaskInterruptedException)
    }

    val now = new Timestamp((new Date).getTime())
    retryable.foreach { registeredTask =>
      registeredTask.nextTry.foreach { nextTry =>
        val waitTime = nextTry.getTime - now.getTime
        if (waitTime < 0) {
          handleTaskException(registeredTask.task, registeredTask.id, registeredTask.tried, PersistentTaskRunner.ScheduleMissedException(nextTry))
        } else {
          val delay = Duration(waitTime, "milliseconds")
          scheduleTask(registeredTask.task, registeredTask.id, delay, registeredTask.tried)
        }
      }
    }
  }
}

object PersistentTaskRunner {
  sealed trait StoreException extends Throwable
  case class MarkBusyException[A](taskId: A, exception: Throwable) extends StoreException
  case class MarkFailedException[A](taskId: A, exception: Throwable) extends StoreException
  case class UnregisterException[A](taskId: A, exception: Throwable) extends StoreException

  sealed trait RecoveryException extends Throwable
  case class ScheduleMissedException(scheduledOn: Timestamp) extends RecoveryException
  case object TaskInterruptedException extends RecoveryException
}
