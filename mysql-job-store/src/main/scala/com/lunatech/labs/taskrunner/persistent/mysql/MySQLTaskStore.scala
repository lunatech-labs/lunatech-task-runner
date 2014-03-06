package com.lunatech.labs.taskrunner.persistent.mysql

import com.lunatech.labs.taskrunner.persistent.TaskStore
import com.lunatech.labs.taskrunner.persistent.TaskStore.RegisteredTask
import java.sql.Timestamp
import java.util.Date
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MySQLTaskStore[A](serialize: A ⇒ (String, String), deserialize: (String, String) ⇒ A)(implicit ec: ExecutionContext) extends TaskStore[A] {
  import MySQLTaskStore.Record

  override type Id = Long

  object TaskSchema extends Schema {
    val tasks = table[Record]

    on(tasks) (task => declare(
      task.id is (autoIncremented),
      task.lastExceptionMessage is(dbType("text")),
      task.lastExceptionStackTrace is(dbType("text"))))
  }

  override def register(task: A, tryAt: Option[Timestamp] = None) = syncFuture {
    transaction {
      val (taskType, taskIdentifier) = serialize(task)
      val busy = tryAt.isEmpty
      val lastRun = if (busy) Some(now) else None
      val record = Record(0, taskType, taskIdentifier, busy, retries = 0, nextTry = tryAt, lastRun = lastRun)
      val insertedRecord = TaskSchema.tasks.insert(record)
      insertedRecord.id
    }
  }

  override def markBusy(id: Long): Future[Unit] = syncFuture {
    transaction {
      TaskSchema.tasks.update(task ⇒
        where(task.id === id)
          set (task.busy := true, task.lastRun := Some(now), task.nextTry := None))
    }
    ()
  }

  override def markFailed(id: Long, exception: Throwable, nextTry: Option[Timestamp]): Future[Unit] = syncFuture {
    transaction {
      TaskSchema.tasks.update(task ⇒
        where(task.id === id)
          set (
            task.busy := false,
            task.nextTry := nextTry,
            task.lastExceptionMessage := Option(exception.getMessage),
            task.lastExceptionStackTrace := Option(exception.getStackTraceString)))
    }
    ()
  }

  override def unregister(id: Long): Future[Unit] = syncFuture {
    transaction {
      TaskSchema.tasks.delete(id)
    }
    ()
  }

  override def listRetryable: Future[Seq[RegisteredTask[A, Long]]] = syncFuture {
    transaction {
      from(TaskSchema.tasks)(task ⇒
        where(task.nextTry.isNotNull)
          select (task)).toList.map { getRegisteredTask }
    }
  }

  override def listBusy: Future[Seq[RegisteredTask[A, Long]]] = syncFuture {
    transaction {
      from(TaskSchema.tasks)(task ⇒
        where(task.busy === true)
          select (task)).toList.map { getRegisteredTask }
    }
  }

  def getSchema: String = {
    val ddlBuilder = new StringBuilder
    TaskSchema.printDdl { line =>
      ddlBuilder append line
      ()
    }
    ddlBuilder.toString
  }

  private def now: Timestamp = new Timestamp((new Date).getTime)

  private def getRegisteredTask(record: Record): RegisteredTask[A, Long] = {
    val task = deserialize(record.taskType, record.taskIdentifier)
    RegisteredTask(task, record.id, record.busy, record.retries, record.lastExceptionMessage, record.lastExceptionStackTrace, record.nextTry)
  }

  /**
   * Create a future from a synchronous computation. Async future's might not work well with Squeryl's threadlocal sessions
   */
  private def syncFuture[A](a: => A): Future[A] = try {
    Future.successful(a)
  } catch {
    case NonFatal(e) => {
      Future.failed(e)
    }
  }
}

object MySQLTaskStore {
  case class Record(id: Long, taskType: String, taskIdentifier: String, busy: Boolean = false, retries: Int, lastRun: Option[Timestamp], lastExceptionMessage: Option[String] = None, lastExceptionStackTrace: Option[String] = None, nextTry: Option[Timestamp] = None) extends KeyedEntity[Long]
}
