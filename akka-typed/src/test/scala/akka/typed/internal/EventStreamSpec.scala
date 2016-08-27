/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal

import scala.concurrent.duration._
import akka.util.Timeout
import akka.event.Logging._
import akka.typed.ScalaDSL._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.Executor
import org.scalatest.concurrent.Eventually

object EventStreamSpec {
  @volatile var logged = Vector.empty[LogEvent]

  class MyLogger extends Logger {
    def initialBehavior: Behavior[Logger.Command] =
      ContextAware { ctx ⇒
        Total {
          case Logger.Initialize(es, replyTo) ⇒
            replyTo ! ctx.watch(ctx.spawn(Static { (ev: LogEvent) ⇒ logged :+= ev }, "logger"))
            Empty
        }
      }
  }

  val config = ConfigFactory.parseString("""
akka.typed.loggers = ["akka.typed.internal.EventStreamSpec$MyLogger"]
""")
}

class EventStreamSpec extends TypedSpec(EventStreamSpec.config) with Eventually {
  import EventStreamSpec._

  object `An EventStreamImpl` {

    def `must work in full system`(): Unit = {
      val es = nativeSystem.eventStream
      es.logLevel should ===(WarningLevel)
      nativeSystem.log.error("hello world")
      nativeSystem.log.debug("should not see this")
      es.setLogLevel(DebugLevel)
      es.logLevel should ===(DebugLevel)
      nativeSystem.log.debug("hello world DEBUG")
      nativeSystem.log.info("hello world INFO")

      eventually(logged.map(_.message) should ===(Vector("hello world", "hello world DEBUG", "hello world INFO")))
      logged = Vector.empty
    }

  }

}
