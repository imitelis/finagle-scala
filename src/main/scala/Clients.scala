// Client
def newService(dest: Name, label: String): Service[Req, Rep]

def newClient(dest: Name, label: String): ServiceFactory[Req, Rep]

import com.twitter.finagle.{Service, ServiceFactory}
import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future

val sessionFactory: ServiceFactory[Request, Response] = Http.client.newClient("example.com:80")

// we establish a session, represented by `svc`
sessionFactory().onSuccess { svc: Service[Request, Response] =>

  // both requests will land on the same host
  val rep1: Future[Response] = svc(Request("/some/path"))
  val rep2: Future[Response] = svc(Request("/some/other/path"))

  // clean up the session so the connection is released into the pool
  svc.close()
}

// session establishment is load balanced. No guarantee as to which endpoint is selected by the load balancer
sessionFactory().onSuccess { ... }

// Client Protocol Implementation
import com.twitter.finagle.Service
import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}

val twitter: Service[Request, Response] = Http.client.newService("twitter.com")

// Transport Security
import com.twitter.finagle.{Service, Http}
import com.twitter.finagle.http.{Request, Response}

val twitter: Service[Request, Response] = Http.client
  .withTransport.tls("twitter.com")
  .newService("twitter.com:443")

import com.twitter.finagle.{Service, Http}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.http.SpnegoAuthenticator.ClientFilter
import com.twitter.finagle.http.SpnegoAuthenticator.Credentials.{ClientSource, JAASClientSource}

val jaas: ClientSource = new JAASClientSource(
  loginContext = "com.sun.security.jgss.krb5.initiate",
  _serverPrincipal = "HTTP/SOME_HOST@SOME_DOMAIN"
)

val client: Service[Request, Response] =
  new ClientFilter(jaas).andThen(Http.client.newService("host:port"))

// HTTP Proxy
import com.twitter.finagle.{Service, Http}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.client.Transporter
import java.net.SocketAddress

val twitter: Service[Request, Response] = Http.client
  .withTransport.httpProxyTo(
    host = "twitter.com:443",
    credentials = Transporter.Credentials("user", "password")
  )
  .newService("inet!my-proxy-server.com:3128") // using local DNS to resolve proxy

// SOCKS5 Proxy
-com.twitter.finagle.socks.socksProxyHost=localhost \
-com.twitter.finagle.socks.socksProxyPort=50001 \
-com.twitter.finagle.socks.socksUsername=$TheUsername \
-com.twitter.finagle.socks.socksPassword=$ThePassword \

// Observability
import com.twitter.finagle.Service
import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Monitor

val monitor: Monitor = new Monitor {
  def handle(t: Throwable): Boolean = {
    // do something with the exception
    true
  }
}

val twitter: Service[Request, Response] = Http.client
  .withMonitor(monitor)
  .newService("twitter.com")

// Retries
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.service.{Backoff, RetryBudget}

val budget: RetryBudget = ???

val twitter = Http.client
  .withRetryBudget(budget)
  .withRetryBackoff(Backoff.const(10.seconds))
  .newService("twitter.com")

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.service.RetryBudget

val budget: RetryBudget = RetryBudget(
  ttl = 10.seconds,
  minRetriesPerSec = 5,
  percentCanRetry = 0.1
)

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.service.{RetryBudget, RetryFilter, RetryPolicy}
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.util.Try

val twitter = Http.client.newService("twitter.com")
val budget: RetryBudget = RetryBudget()
val policy: RetryPolicy[Try[Nothing]] = ???

val retry = new RetryFilter(
  retryPolicy = policy,
  timer = DefaultTimer,
  statsReceiver = NullStatsReceiver,
  retryBudget = budget
)

val retryTwitter = retry.andThen(twitter)

import com.twitter.finagle.http.{Response, Status}
import com.twitter.finagle.service.{Backoff, RetryPolicy}
import com.twitter.util.{Try, Return, Throw}
import com.twitter.conversions.DurationOps._

val policy: RetryPolicy[Try[Response]] =
  RetryPolicy.backoff(Backoff.equalJittered(10.milliseconds, 10.seconds)) {
    case Return(rep) if rep.status == Status.InternalServerError => true
  }

// Timeouts & Expirations
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http

val twitter = Http.client
  .withSession.acquisitionTimeout(42.seconds)
  .newService("twitter.com")

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http

val twitter = Http.client
  .withRequestTimeout(42.seconds)
  .newService("twitter.com")

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http

val twitter = Http.client
  .withSession.maxLifeTime(20.seconds)
  .newService("twitter.com")

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future

val response: Future[Response] = twitter(request).within(1.second)

// Load Balancer
import com.twitter.finagle.Http
import com.twitter.finagle.loadbalancer.LoadBalancerFactory

val balancer: LoadBalancerFactory = ???
val twitter = Http.client
  .withLoadBalancer(balancer)
  .newService("twitter.com:8081,twitter.com:8082")

// Heap + Least Loaded
import com.twitter.finagle.loadbalancer.{Balancers, LoadBalancerFactory}

val balancer: LoadBalancerFactory = Balancers.heap()

// Power of Two Choices (P2C)
import com.twitter.finagle.loadbalancer.{Balancers, LoadBalancerFactory}

val balancer: LoadBalancerFactory = Balancers.p2c()

// P2C + Peak EWMA
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.loadbalancer.{Balancers, LoadBalancerFactory}

val balancer: LoadBalancerFactory =
  Balancers.p2cPeakEwma(decayTime = 100.seconds)

// Aperture + Least Loaded
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.loadbalancer.{Balancers, LoadBalancerFactory}

val balancer: LoadBalancerFactory =
  Balancers.aperture(
    smoothWin = 32.seconds,
    lowLoad = 1.0,
    highLoad = 2.0,
    minAperture = 10
  )

// Panic Mode
import com.twitter.finagle.loadbalancer.PanicMode

val client = Http.client
  .withLoadBalancer.panicMode(PanicMode.FortyPercentUnhealthy)

// Fail Fast
import com.twitter.finagle.Http

val twitter = Http.client
  .withSessionQualifier.noFailFast
  .newService("twitter.com")

// Failure Accrual
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.liveness.{FailureAccrualFactory, FailureAccrualPolicy}
import com.twitter.finagle.service.Backoff

val twitter = Http.client
  .configured(FailureAccrualFactory.Param(() => FailureAccrualPolicy.successRate(
    requiredSuccessRate = 0.95,
    window = 100,
    markDeadFor = Backoff.const(10.seconds)
  )))
  .newService("twitter.com")

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.liveness.{FailureAccrualFactory, FailureAccrualPolicy}
import com.twitter.finagle.service.Backoff

val twitter = Http.client
  .configured(FailureAccrualFactory.Param(() => FailureAccrualPolicy.successRateWithinDuration(
    requiredSuccessRate = 0.95,
    window = 5.minutes,
    markDeadFor = Backoff.const(10.seconds),
    minRequestThreshold = 100
  )))
  .newService("twitter.com")

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.liveness.{FailureAccrualFactory, FailureAccrualPolicy}
import com.twitter.finagle.service.Backoff

val twitter = Http.client
  .configured(FailureAccrual.Param(() => FailureAccrualPolicy.consecutiveFailures(
    numFailures = 10,
    markDeadFor = Backoff.const(10.seconds)
  )))
  .newService("twitter.com")

import com.twitter.finagle.Http

val twitter = Http.client
  .withSessionQualifier.noFailureAccrual
  .newService("twitter.com")

// Pooling
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http

val twitter = Http.client
  .withSessionPool.minSize(10)
  .withSessionPool.maxSize(20)
  .withSessionPool.maxWaiters(100)
  .withSessionPool.ttl(5.seconds)
  .newService("twitter.com")

// Admission Control
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http

val twitter = Http.client
  .withAdmissionControl.nackAdmissionControl(
    window = 10.minutes,
    nackRateThreshold = 0.75
  )

// Response Classification
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.thrift.service.ThriftResponseClassifier

ThriftMux.client
  ...
  .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)

import com.twitter.finagle.Http
import com.twitter.finagle.http.service.HttpResponseClassifier

Http.server
  ...
  .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)

// Custom Classifiers
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

// Thrift and ThriftMux Classifiers
exception NotFoundException { 1: string reason }
exception InvalidQueryException {
  1: i32 errorCode
}

service SocialGraph {
  i32 follow(1: i64 follower, 2: i64 followee) throws (
    1: NotFoundException ex1,
    2: InvalidQueryException ex2
  )
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