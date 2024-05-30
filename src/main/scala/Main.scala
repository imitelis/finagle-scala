import com.twitter.finagle.{Http, Service, SimpleFilter, Address, Name}
import com.twitter.finagle.http.{Request, Response, Method, Status}
import com.twitter.util.{Await, Future, Duration, Timer, JavaTimer}

object Main { // RPC
    // Service[A, B] = A => Future[B]
    def debugFilter(id: String) = new SimpleFilter[Request, Response] {
        override def apply(request: Request, service: Service[Request, Response]) = {
            println(s"[${id}] received request $request")
            service(request)
        }
    }

    // GET localhost:9090?name=daniel
    def stringLengthService = new Service[Request, Response] {
        override def apply(request: Request): Future[Response] = Future {
            val result = Option(request.getParam("name")).map(_.length).getOrElse(-1)
            val response = Response(Status.Ok)
            response.setContentString(result.toString)
            Thread.sleep(1200)
            response
        }
    }

    def simpleHttpServer(port: Int) = 
        Http.serve(s":${port}", debugFilter(s"server-$port").andThen(stringLengthService))

    def main(args: Array[String]): Unit = {
        (9090 to 9093).map {
            port => simpleHttpServer(port)
        }.foreach(server => Await.ready(server))
        // (simpleHttpServer)
    }
}

object SimpleClient {
    def main(args: Array[String]): Unit = {
        val originalClient: Service[Request, Response] = Http.newService("localhost:9090")
        val timeoutFilter = new TimoutFilter[Request, Response](Duration.fromSeconds(1), new JavaTimer())
        val filteredClient = timeoutFilter.andThen(originalClient)
        val request = Request(Method.Get, "/?name=daniel")
        val response: Future[Response] = filteredClient(request)
        // asynchronous API
        // map, flatMap, for comprehensions
        response.onSuccess(resp => println(resp.getContentString()))
        response.onFailure(ex => ex.printStackTrace())
        Thread.sleep(2000)
    }
}

// load balancer
object LoadBalancedClient {
    def main(args: Array[String]): Unit = {
        val addresses = (9090 to 9097).toList.map(port => Address("localhost", port))
        val name: Name = Name.bound(addresses: _*)
        val client = Http.newService(name, "client")
        val requests = (1 to 20).map(i => Request(Method.Get, s"?name=${"daniel" * i}"))
        val responses = requests.map(req => client(req))
        // "traverse"
        Future.collect(responses).foreach(println)
        Thread.sleep(5000)
        // Name
    }
}

// filters = "middleware"
/* 
ReqIn -> | FILTER | -> RepOut | SERVICE |
RepIn <- | FILTER | <- ReqOut | SERVICE |

filter input = ReqIn => RepOut
service = RepOut => ReqOut
filter output = ReqOut => RepIn

filter[ReqIn, RepOut, ReqOut, RepIn]
    apply(req: ReqIn, service: Service[RepOut, ReqOut]): Future[ReqOut]

SimpleFilter[Req, Rep] => Filter[Req, Rep, Req, Rep]
*/

class TimoutFilter[Req, Rep](timeout: Duration, timer: Timer) extends SimpleFilter[Req, Rep] {
    override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] =
        service(request).within(timer, timeout)
}