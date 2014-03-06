package com.lunatech.labs.taskrunner

import scala.concurrent.Future

/**
 * Typeclass for tasks
 */
trait Task[A] {
  def run(a: A): Future[Unit]
}
