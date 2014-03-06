package com.lunatech.labs.taskrunner.persistent.mysql

import com.lunatech.labs.taskrunner.persistent.TaskStoreTests
import com.lunatech.labs.taskrunner.persistent.TaskStore
import scala.concurrent.ExecutionContext.Implicits.global
import org.squeryl.SessionFactory
import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.PrimitiveTypeMode._

sealed trait TestTask
case class PrintTask(id: String) extends TestTask

class MySQLTaskStoreTests extends TaskStoreTests {
  sequential

  Class.forName("com.mysql.jdbc.Driver")
  SessionFactory.concreteFactory = Some { ()=>
    Session.create( java.sql.DriverManager.getConnection("jdbc:mysql://localhost/test", "test", "test"),new MySQLAdapter)
  }

  def serializeTask(task: TestTask) = task match {
    case PrintTask(id) => ("PrintTask", id)
  }

  def deserializeTask(s1: String, s2: String) = s1 match {
    case "PrintTask" => PrintTask(s2)
  }

  override type Task = TestTask

  override def generateTask = new PrintTask("42")

  override def runWithStore[A](example: TaskStore[Task] => A): A = {

    val taskStore = new MySQLTaskStore[TestTask](serializeTask, deserializeTask)
    try {
    transaction { taskStore.TaskSchema.create }
    val result = example(taskStore)
    result
  } finally {
    transaction { taskStore.TaskSchema.drop }
  }
  }

}
