package com.lunatech.labs.taskrunner.persistent

import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.Await
import scala.concurrent.Future

abstract class TaskStoreTests extends Specification {

  type Task
  type ListType[Id] = Seq[TaskStore.RegisteredTask[Task, Id]]

  def runWithStore[A](example: TaskStore[Task] => A): A

  def generateTask: Task

  def wait[A](f: Future[A]): A = Await.result(f, Duration(3, "seconds"))

  "The taskstore" should {
    "list a task as busy if it's registered to run now" in runWithStore { taskStore =>
      wait(taskStore.register(generateTask))
      taskStore.listBusy must haveSize[ListType[taskStore.Id]](1).await
      taskStore.listRetryable must haveSize[ListType[taskStore.Id]](0).await
    }

    "list a task as retryable if it's registered to run later" in runWithStore { taskStore =>
      wait(taskStore.register(generateTask, Some(new Timestamp((new Date).getTime))))

      taskStore.listBusy must haveSize[ListType[taskStore.Id]](0).await
      taskStore.listRetryable must haveSize[ListType[taskStore.Id]](1).await
    }

    "list a task as busy if it was registered first and then marked busy" in runWithStore { taskStore =>
      Await.result(taskStore.register(generateTask).flatMap { taskId =>
        taskStore.markBusy(taskId)
      }, Duration(1, "seconds"))

      taskStore.listBusy must haveSize[ListType[taskStore.Id]](1).await
      taskStore.listRetryable must haveSize[ListType[taskStore.Id]](0).await
    }

    "list a task as retryable if it was marked as failed and rescheduled" in runWithStore { taskStore =>
      Await.result(taskStore.register(generateTask).flatMap { taskId =>
        taskStore.markFailed(taskId, new RuntimeException("Foo!"), Some(new Timestamp((new Date).getTime)))
      }, Duration(1, "seconds"))

      taskStore.listBusy must haveSize[ListType[taskStore.Id]](0).await
      taskStore.listRetryable must haveSize[ListType[taskStore.Id]](1).await
    }

    "unregister a task that was marked as permanently failed" in runWithStore { taskStore =>
      Await.result(taskStore.register(generateTask).flatMap { taskId =>
        taskStore.markFailed(taskId, new RuntimeException("Foo!"), None)
      }, Duration(1, "seconds"))

      taskStore.listBusy must haveSize[ListType[taskStore.Id]](0).await
      taskStore.listRetryable must haveSize[ListType[taskStore.Id]](0).await
    }
  }

}
