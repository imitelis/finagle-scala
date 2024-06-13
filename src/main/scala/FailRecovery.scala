trait Future[A] {
  ..
  def rescue[B >: A](f: PartialFunction[Throwable, Future[B]]): Future[B]
  ..
}

import com.twitter.util.Future
import com.twitter.finagle.http
import com.twitter.finagle.TimeoutException

def fetchUrl(url: String): Future[http.Response] = ???

def fetchUrlWithRetry(url: String): Future[http.Response] =
  fetchUrl(url).rescue {
    case exc: TimeoutException => fetchUrlWithRetry(url)
  }

val original: Future[Tweet] = ???
val hedged: Future[Tweet] = ???
// Future#select[U >: A](Future[U]): Future[U]
val fasterTweet = original.select(hedged)
