package com.lunatech.labs.taskrunner.retrystrategies

import com.lunatech.labs.taskrunner.RetryStrategy
import com.lunatech.labs.taskrunner.persistent.PersistentTaskRunner.ScheduleMissedException
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * Retry strategy that takes a list of wait times between tries.
 *
 * This strategy will retry tasks that fail with a `RetryableBackoffStrategy.Retryable` exception.
 * If `allowMissedSchedule` is defined if will also retry a `ScheduleMissedException` if it's not more passed
 * the scheduled time than the value in `allowMissedSchedule`.
 */
class RetryableBackoffStrategy(waitTimes: List[FiniteDuration], allowMissedSchedule: Option[FiniteDuration] = None) extends RetryStrategy[Any] {
  import RetryableBackoffStrategy._

  override def nextRetryDelay(task: Any, tried: Int, throwable: Throwable) = throwable match {
    case Retryable(_) => waitTimes.lift(tried - 1)
    case ScheduleMissedException(scheduledOn) if isMissedReschedulable(scheduledOn) => Some(Duration(0, "seconds"))
    case other => None
  }

  private def isMissedReschedulable(scheduleOn: Timestamp): Boolean = allowMissedSchedule match {
    case Some(maxExceed) => {
      val maxExecutionTimestamp = new Timestamp(scheduleOn.getTime() + maxExceed.toMillis)
      val now = new Timestamp((new Date).getTime)
      val allowExecution = maxExecutionTimestamp after now
      allowExecution
    }
    case None => false
  }
}

object RetryableBackoffStrategy {
  /**
   * Throwables of this type are considered retryable
   */
  case class Retryable(underlying: Throwable) extends Throwable(underlying)

}
