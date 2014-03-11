package com.lunatech.labs.taskrunner.persistent

import java.sql.Timestamp
import scala.concurrent.Future

/**
 * A = task, B = id
 *
 *



- create & busy
- run & fail
-

- report error & unbusy

 (wait)
- busy
- run again
- report error & unbusy
 (wait)

- run again
- run error
 *
 */
trait TaskStore[A] {
  type Id

  import TaskStore._

  /**
   * Registers a task in persistent storage.
   *
   * If `runAt` is defined, the task will be scheduled and not marked as busy.
   * If `runAt` is not defined, the task will be marked busy.
   */
  def register(task: A, runAt: Option[Timestamp] = None): Future[Id]

  /**
   * Mark an existing task as busy.
   */
  def markBusy(id: Id): Future[Unit]

  /**
   * Mark a task as failed. Removes the `busy` flag.
   *
   * A `nextTry` of `None` indicates a fatal error.
   */
  def markFailed(id: Id, exception: Throwable, nextTry: Option[Timestamp]): Future[Unit]

  /**
   * Unregister a task.
   */
  def unregister(id: Id): Future[Unit]

  /**
   * Return all registered tasks that are retryable, meaning that they have a nextTry that is a `Some`
   */
  def listRetryable: Future[Seq[RegisteredTask[A, Id]]]

  /**
   * Return all busy tasks
   */
  def listBusy: Future[Seq[RegisteredTask[A, Id]]]

}

object TaskStore {
  case class RegisteredTask[A, B](
    task: A,
    /**
     * Unique id generated by the task store
     */
    id: B, busy: Boolean = false, tried: Int = 0, lastExceptionMessage: Option[String] = None, lastExceptionStackTrace: Option[String] = None, nextTry: Option[Timestamp] = None)
}

