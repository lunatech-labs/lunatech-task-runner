package com.lunatech.labs.taskrunner

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait TaskRunner[A] {
  /**
   * Run a task asynchronously
   *
   * @param task the task to run
   * @param delay delay before running this task. If `None`, run the task immediately
   */
  def runTask(task: A, delay: Option[FiniteDuration] = None): Future[Unit]
}
