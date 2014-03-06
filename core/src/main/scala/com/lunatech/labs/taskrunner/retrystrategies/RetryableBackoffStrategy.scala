package com.lunatech.labs.taskrunner.retrystrategies

import com.lunatech.labs.taskrunner.RetryStrategy
import scala.concurrent.duration.FiniteDuration

/**
 * Retry strategy that takes a list of wait times between tries.
 *
 * This strategy will retry tasks that fail with a `RetryableBackoffStrategy.Retryable` exception
 */
class RetryableBackoffStrategy(waitTimes: List[FiniteDuration]) extends RetryStrategy[Any] {
  import RetryableBackoffStrategy._

  override def nextRetryDelay(task: Any, retries: Int, throwable: Throwable) = throwable match {
    case Retryable(_) => waitTimes.lift(retries)
    case other => None
  }
}

object RetryableBackoffStrategy {
  /**
   * Throwables of this type are considered retryable
   */
  case class Retryable(underlying: Throwable) extends Throwable(underlying)

}
