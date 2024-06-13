// Filter
abstract class Filter[-ReqIn, +RepOut, +ReqOut, -RepIn] extends ((ReqIn, Service[ReqOut, RepIn]) => Future[RepOut])

trait SimpleFilter[Req, Rep] extends Filter[Req, Rep, Req, Rep]

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.{Duration, Future, Timer}

class TimeoutFilter[Req, Rep](timeout: Duration, timer: Timer)
  extends SimpleFilter[Req, Rep] {

  def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    val res = service(request)
    res.within(timer, timeout)
  }
}

// Composition with Services
import com.twitter.finagle.Service
import com.twitter.finagle.http

val service: Service[http.Request, http.Response] = ...
val timeoutFilter = new TimeoutFilter[http.Request, http.Response](...)

val serviceWithTimeout: Service[http.Request, http.Response] =
  timeoutFilter.andThen(service)

import com.twitter.finagle.Filter
import com.twitter.finagle.service.RetryFilter

def retry[Req, Rep]: RetryFilter[Req, Rep] = ???
def retryWithTimeoutFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] =
  retry[Req, Rep].andThen(new TimeoutFilter[Req, Rep](...))

// ServiceFactory
abstract class ServiceFactory[-Req, +Rep]
  extends (ClientConnection => Future[Service[Req, Rep]])