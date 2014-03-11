package com.lunatech.labs.taskrunner

import scala.concurrent.duration.FiniteDuration

trait RetryStrategy[-A] {
  def nextRetryDelay(task: A, tried: Int, throwable: Throwable): Option[FiniteDuration]
}
