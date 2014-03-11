package com.lunatech.labs.taskrunner.retrystrategies

import com.lunatech.labs.taskrunner.RetryStrategy

object NoRetries extends RetryStrategy[Any] {
  override def nextRetryDelay(task: Any, tried: Int, throwable: Throwable) = None
}
