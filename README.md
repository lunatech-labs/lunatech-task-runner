TaskRunner
==========

Task runner is a Scala library that provides an abstraction for running asynchronous tasks. Asynchronous tasks are useful in web programming, because it allows us to quickly respond with a succesful status to a users request, even though performing all the work the user requested may take a long time.

This library intends to solve the problems of _retrying_ and _persisting_ asynchronous tasks, that pop up when doing work asynchronously.

Quick Start
-----------

In your `build.sbt`:

    resolvers += "Lunatech Releases" at "http://artifactory.lunatech.com/artifactory/releases-public/"
    
    libraryDependencies += "com.lunatech.task-runner" %% "task-runner-core" % "0.1"
    
If you want the MySQL Squeryl task store, you can use:

    libraryDependencies += "com.lunatech.task-runner" %% "task-runner-squeryl-mysql-store" % "0.1"

Problem description
-------------------
Suppose that we have a webservice, where people can register a user account. Upon registration, the webservice will do a request to an system to store the customer data.

At some point in time, our system should return a _success_ status back to the client, indicating that the data was received. After giving this success status, our webservice is responsible for the data, until it succesfully sends it to the external system and retrieves a _success_ status from that system.

The easiest way to solve this, is to make our client wait until our webservice registered the data at the external service. That means our service gets a success status from the external service _before_ it sends one to the client. That's great, because it means that the data is _never_ the responsibility of our service! Awesome!

One disadvantage of this approach that the speed with which our service can give a confirmation to the user is limited by how fast it can get one from the remote service.

Another one is that when the external system is unavailable, our webservice is effectively unavailable as well. If we can't get a confirmation, we won't give one to our client.

Often, we want the availability or performance of our service not to be limited by the availability or performance of the external service.

So what we can do is to take _responsibility_ of the data, by sending a _success_ status to our client as soon as we've received the data. Then, _asynchronously_, we'll send it to the external service.

This introduces two needs into our system:

 * _retrying_: When the external service is down, we need to retry sending our data to it at a later point in time.
 * _persistence_: Now that we're responsible for the data, we don't want it to be gone if our system is shut down or crashes. Often, we're trying to keep our service _stateless_, which it's not anymore if there's tasks hanging around in memory.

This library helps to deal with these needs by introducing an abstraction for running asynchronous tasks:

    trait TaskRunner[A] {
      def runTask(task: A, delay: Option[FiniteDuration]): Future[Unit]
    }

It also provides two implementations of this trait. A `BasicTaskRunner`, which is capable of _retrying_, but doesn't do persistence. And a `PersistentTaskRunner`, which is capable of _retrying_ and _persisting_.

BasicTaskRunner
---------------

This task runner can retry tasks, but doesn't persist them. They're gone if the application is stopped.

This task runner can run any sort of task, as long as it has the `Task` typeclass implemented:

    trait Task[A] {
      def run(a: A): Future[Unit]      
    }

PersistentTaskRunner
--------------------

The persistent task runner can retry tasks, and persists them in a pluggable storage backend.

This adds some additional constraints on the type of tasks that can be run, because the system must be able to persist and restore them.

To persist tasks, a `PersistentTaskRunner` uses a `TaskStore`, an abstraction for storing tasks. Naturally, this `TaskStore` must be able to serialize a task somehow and be able to restore it.

Retrying
--------

TODO, write about `RetryStrategy`


