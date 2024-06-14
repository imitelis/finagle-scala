scala> import com.twitter.conversions.DurationOps._, com.twitter.finagle.thrift.ClientId, com.twitter.finagle.util.HashedWheelTimer, com.twitter.util.{Await, Future}
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.util.HashedWheelTimer
import com.twitter.util.{Await, Future}

scala> val aClientId = ClientId("test-client") // Create a ClientId to use
aClientId: com.twitter.finagle.thrift.ClientId = ClientId(test-client)

scala> val clientIdInTimer: Future[String] =
         // Put that ClientId into scope and schedule
         // work on a Timer
         aClientId.asCurrent {
           HashedWheelTimer.Default.doLater(100.milliseconds) {
             // Check the value when the Timer's function is
             // evaluated 100 milliseconds later
             ClientId.current match {
               case Some(cId) => cId.name
               case None => "no-client-id"
             }
           }
         }
clientIdInTimer: com.twitter.util.Future[String] = Promise@2147108131(state=Interruptible(List(),<function1>))

scala> println(s"Timer saw: '${Await.result(clientIdInTimer)}'")
Timer saw: 'test-client'