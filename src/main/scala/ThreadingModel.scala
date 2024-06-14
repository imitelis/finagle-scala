-com.twitter.finagle.netty4.numWorkers=24

// Blocking Examples
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future

class MyHttpService extends Service[Request, Response] {
  def apply(req: Request): Future[Response] = {
    val rep = Response()
    rep.contentString = req.contentString.permutations.mkString("\n")

    Future.value(rep)
  }
}

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}

def process(client: Service[Request, Response]): Future[String] =
  client(Request()).map(rep => rep.contentString.permutations.mkString("\n"))

// Offloading
import com.twitter.util.{Future, FuturePool}

def offloadedPermutations(s: String, pool: FuturePool): Future[String] =
  pool(s.permutations.mkString("\n"))

import com.twitter.util.FuturePool
import com.twitter.finagle.Http

val server: Http.Server = Http.server
  .withExecutionOffloaded(FuturePool.unboundedPool)

val client: Http.Client = Http.client
  .withExecutionOffloaded(FuturePool.unboundedPool)

-com.twitter.finagle.offload.auto=true

-com.twitter.finagle.offload.numWorkers=14 -com.twitter.finagle.netty4.numWorkers=10

// Offload Admission Control
-com.twitter.finagle.offload.admissionControl=enabled

-com.twitter.finagle.offload.admissionControl=50.milliseconds