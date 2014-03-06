package com.lunatech.labs.taskrunner

import scala.concurrent.duration.FiniteDuration

trait RetryStrategy[-A] {
  def nextRetryDelay(task: A, retries: Int, throwable: Throwable): Option[FiniteDuration]
}
