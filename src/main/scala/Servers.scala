// Server
def serve(
  addr: SocketAddress,
  factory: ServiceFactory[Req, Rep]
): ListeningServe

import com.twitter.finagle.Service
import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, Future}

val service: Service[Request, Response] = new Service[Request, Response] {
  def apply(req: Request): Future[Response] = Future.value(Response())
}

val server = Http.server.serve(":8080", service)
Await.ready(server) // waits until the server resources are released

// Response Classification
com.twitter.finagle.ThriftMux
import com.twitter.finagle.thrift.service.ThriftResponseClassifier

ThriftMux.client
  ...
  .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)

import com.twitter.finagle.Http
import com.twitter.finagle.http.service.HttpResponseClassifier

Http.server
  ...
  .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)

val rc: ResponseClassifier = {
  case ReqRep(req, Throw(exc)) => ResponseClass.RetryableFailure
  case ReqRep(req, Return(rep)) => ResponseClass.Success
}

import com.twitter.finagle.http
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.util.Return

val classifier: ResponseClassifier = {
  case ReqRep(_, Return(r: http.Response)) if r.statusCode == 503 =>
    ResponseClass.NonRetryableFailure
}

import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}

val classifier: ResponseClassifier = {
  // #1
  case ReqRep(_, Throw(_: NotFoundException)) =>
    ResponseClass.NonRetryableFailure

  // #2
  case ReqRep(_, Return(x: Int)) if x == 0 =>
    ResponseClass.NonRetryableFailure

  // #3 *Caution*
  case ReqRep(SocialGraph.Follow.Args(a, b), _) if a <= 0 =>
    ResponseClass.NonRetryableFailure

  // #4
  case ReqRep(_, Throw(_: InvalidQueryException)) =>
    ResponseClass.Success
}

// Concurrency Limit
import com.twitter.finagle.Http

val server = Http.server
  .withAdmissionControl.concurrencyLimit(
    maxConcurrentRequests = 10,
    maxWaiters = 0
  )
  .serve(":8080", service)

// Rejecting Requests
import com.twitter.finagle.Failure

val rejection = Future.exception(Failure.rejected("busy"))
val nonRetryable = Future.exception(Failure("Don't try again", Failure.Rejected|Failure.NonRetryable))

// Session Expiration
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http

val twitter = Http.server
  .withSession.maxLifeTime(20.seconds)
  .withSession.maxIdleTime(10.seconds)
  .newService("twitter.com")