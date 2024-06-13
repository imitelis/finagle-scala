import com.twitter.util.Future

val original: Future[Tweet] = ???
val hedged: Future[Tweet] = ???
// Future#select[U >: A](Future[U]): Future[U]
val fasterTweet = original.select(hedged)

import com.twitter.util.Future
import com.twitter.util.Try
import java.util.concurrent.CancellationException

val doWork: () => Future[Tweet] = ???
val tweets: Seq[Future[Tweet]] = Seq.fill(10)(doWork)
// Future.select[A](Seq[Future[A]]): Future[(Try[A], Seq[Future[A]])]
val first: Future[Tweet] = Future.select(tweets).flatMap {
  case (first, rest) =>
    val cancelEx = new CancellationException("lost the race")
    rest.foreach { f => f.raise(cancelEx) }
    Future.const(first)
}

// Process as much as you can
val doWork: () => Future[Tweet]
val tweets: Seq[Future[Tweet]] = Seq.fill(10)(doWork)
def tweetSentiment(tweet: Tweet): Int = ???
def aggregateTweetSentiment(f: Future[(Try[Tweet], Seq[Future[Tweet]])]): Future[Seq[Int]] = f match {
  case (first, rest) =>
    val (finished, unfinished) = (Future.const(first) +: rest).foldLeft((Seq[Tweet](), Seq[Future[Tweet]]())) {
      case ((complete, incomplete), f) => f.poll match {
        case Some(Return(tweet)) => (complete :+ tweet, incomplete)
        case None => (complete, incomplete :+ f)
        case _ => (complete, incomplete) // failed future
      }
    }
    val sentiments = finished.map(tweetSentiment)
    if (unfinished.isEmpty) Future.value(sentiments)
    else Future.select(unfinished).flatMap(aggregateTweetSentiment).map(sentiments ++ _)
}
// Future.select[A](Seq[Future[A]]): Future[(Try[A], Seq[Future[A]])]
val avgTweetSentiment: Future[Int] = Future.select(tweets).flatMap(aggregateTweetSentiment).map { seq =>
  if (seq.isEmpty) 0 else (seq.sum / seq.length)
}

// Going until first successful result
val doWork: () => Future[Tweet]
val tweets: Seq[Future[Tweet]] = Seq.fill(10)(doWork)
def raceTheTweets(f: Future[(Try[Tweet], Seq[Future[Tweet]])]): Future[Tweet] = f match {
  case (Throw(_), rest) if rest.length > 1 => // There was a failure, but there are more futures to await
    Future.select(rest).flatMap(raceTheTweets _)
  case (Throw(_), Seq(last)) => // Only one remaining, we will return it regardless of success
    last
  case (result, _) => // This is either successful or the last Future has failed
    Future.const(result)
}
// Future.select[A](Seq[Future[A]]): Future[(Try[A], Seq[Future[A]])]
val first: Future[Tweet] = Future.select(tweets).flatMap(raceTheTweets _)

// Even more powerful
val doWork: () => Future[Tweet]
val tweets: IndexedSeq[Future[Tweet]] = IndexedSeq.fill(10)(doWork)
// Future.selectIndex[A](IndexedSeq[Future[A]]): Future[Int]
val first: Future[Tweet] = Future.selectIndex(tweets).flatMap { idx =>
  val cancelEx = new CancellationException("lost the race")
  for (i < 0 until 10 if i != idx) tweets(i).raise(cancelEx)
  tweets(idx)
}