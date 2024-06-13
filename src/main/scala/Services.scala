// Service
trait Service[Req, Rep] extends (Req => Future[Rep])

import com.twitter.finagle.Service
import com.twitter.finagle.http
import com.twitter.util.Future

// Client
val httpService: Service[http.Request, http.Response] = ???

httpService(Request("/foo/bar")).onSuccess { response: http.Response =>
  println("received response " + response.contentString)
}

// Server
val httpService = new Service[http.Request, http.Response] {
  def apply(req: http.Request): Future[http.Response] =
    Future.value(Response()) // HTTP 200
}
