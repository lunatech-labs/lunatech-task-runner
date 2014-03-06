package com.lunatech.labs.taskrunner.persistent

import TaskStore.RegisteredTask
import com.lunatech.labs.taskrunner.persistent.TaskStore.RegisteredTask
import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

class InMemoryTaskStore[A](implicit ec: ExecutionContext) extends TaskStore[A] {
  import TaskStore.RegisteredTask

  var registry: Map[Id, RegisteredTask[A, Id]] = Map()

  type Id = UUID

  override def register(task: A, runAt: Option[Timestamp] = None): Future[Id] = synchronized {
    val id = UUID.randomUUID()
    val registeredTask = runAt match {
      case Some(runAt) => RegisteredTask(task, id, busy = false, nextTry = Some(runAt))
      case None => RegisteredTask(task, id, busy = true)
    }
    registry = registry + (id -> registeredTask)
    Future.successful(id)
  }

  /**
   * Mark an existing task as busy
   */
  def markBusy(id: Id): Future[Unit] = synchronized {
    val registeredTask = registry(id)
    val updatedRegisteredTask = registeredTask.copy(busy = true)
    registry = registry + (id -> updatedRegisteredTask)
    Future.successful(())
  }

  /**
   * Unmark task as busy, and register throwable and next try.
   *
   * A `nextTry` of `None` indicates a fatal error.
   */
  def markFailed(id: Id, exception: Throwable, nextTry: Option[Timestamp]): Future[Unit] = synchronized {
    val registeredTask = registry(id)
    val updatedRegisteredTask = registeredTask.copy(busy = false, nextTry = nextTry, lastExceptionMessage = Some(exception.getMessage), lastExceptionStackTrace = Some(exception.getStackTraceString))
    registry = registry + (id -> updatedRegisteredTask)
    Future.successful(())
  }

  /**
   * Remove a task
   */
  def unregister(id: Id): Future[Unit] = synchronized {
    registry = registry - id
    Future.successful(())
  }

  def listRetryable: Future[Seq[RegisteredTask[A, Id]]] = synchronized {
    Future.successful(registry.values.filter(_.nextTry.isDefined).toSeq)
  }

  def listBusy: Future[Seq[RegisteredTask[A, Id]]] = synchronized {
    Future.successful(registry.values.filter(_.busy).toSeq)
  }
}
