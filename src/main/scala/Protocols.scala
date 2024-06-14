// Using finagle-thrift
service LoggerService {
  string log(1: string message, 2: i32 logLevel) throws (1: WriteException writeEx);
  i32 getLogSize() throws (1: ReadException readEx);
}

val server: ListeningServer = Thrift.server.serveIface(
  "localhost:1234",
  new LoggerService.MethodPerEndpoint {
    def log(message: String, logLevel: Int): Future[String] = {
      println(s"[$logLevel] Server received: '$message'")
      Future.value(s"You've sent: ('$message', $logLevel)")
    }

    var counter = 0
    // getLogSize throws ReadExceptions every other request.
    def getLogSize(): Future[Int] = {
      counter += 1
      if (counter % 2 == 1) {
        println(s"Server: getLogSize ReadException")
        Future.exception(new ReadException())
      } else {
        println(s"Server: getLogSize Success")
        Future.value(4)
      }
    }
  }
)

val clientServicePerEndpoint: LoggerService.ServicePerEndpoint =
  Thrift.client.servicePerEndpoint[LoggerService.ServicePerEndpoint](
    "localhost:1234",
    "thrift_client"
  )

val result: Future[Log.SuccessType] = clientServicePerEndpoint.log(Log.Args("hello", 1))

val uppercaseFilter = new SimpleFilter[Log.Args, Log.SuccessType] {
  def apply(
    req: Log.Args,
    service: Service[Log.Args, Log.SuccessType]
  ): Future[Log.SuccessType] = {
    val uppercaseRequest = req.copy(message = req.message.toUpperCase)
    service(uppercaseRequest)
  }
}

def timeoutFilter[Req, Rep](duration: Duration) = {
  val exc = new IndividualRequestTimeoutException(duration)
  val timer = DefaultTimer
  new TimeoutFilter[Req, Rep](duration, exc, timer)
}
val filteredLog: Service[Log.Args, Log.SuccessType] = timeoutFilter(2.seconds)
  .andThen(uppercaseFilter)
  .andThen(clientServicePerEndpoint.log)

filteredLog(Log.Args("hello", 2))
// [2] Server received: 'HELLO'

val retryPolicy: RetryPolicy[Try[GetLogSize.Result]] =
  RetryPolicy.tries[Try[GetLogSize.Result]](
    3,
    {
      case Throw(ex: ReadException) => true
    })

val retriedGetLogSize: Service[GetLogSize.Args, GetLogSize.SuccessType] =
  new RetryExceptionsFilter(retryPolicy, DefaultTimer)
    .andThen(clientServicePerEndpoint.getLogSize)

retriedGetLogSize(GetLogSize.Args())

val client: LoggerService.MethodPerEndpoint =
  Thrift.client.build[LoggerService.MethodPerEndpoint]("localhost:1234")
client.log("message", 4).onSuccess { response =>
  println("Client received response: " + response)
}

val filteredMethodIface: LoggerService.MethodPerEndpoint =
  Thrift.Client.methodPerEndpoint(clientServicePerEndpoint.withLog(filteredLog))
Await.result(filteredMethodIface.log("ping", 3).map(println))

// Mysql
val client = Mysql.client
  .withCredentials("<user>", "<password>")
  .withDatabase("test")
  .configured(DefaultPool
    .Param(low = 0, high = 10, idleTime = 5.minutes, bufferSize = 0, maxWaiters = Int.MaxValue))
  .newClient("127.0.0.1:3306")

val product = client().flatMap { service =>
  // `service` is checked out from the pool.
  service(QueryRequest("SELECT 5*5 AS `product`"))
    .map {
      case rs: ResultSet => rs.rows.map(processRow)
      case _ => Seq.empty
    }.ensure {
      // put `service` back into the pool.
      service.close()
    }
}

def processRow(row: Row): Option[Long] =
  row.getLong("product")

val richClient = Mysql.client
  .withCredentials("<user>", "<password>")
  .withDatabase("test")
  .configured(DefaultPool
    .Param(low = 0, high = 10, idleTime = 5.minutes, bufferSize = 0, maxWaiters = Int.MaxValue))
  .newRichClient("127.0.0.1:3306")
