import com.twitter.util.{Future, FuturePool}

def someIO(): String =
  // does some blocking I/O and returns a string

val futureResult: Future[String] = FuturePool.unboundedPool {
  someIO()
}

private[this] var counter: Integer = 0
private[this] val lock: Object = counter

def incrementAndReturn(): Integer = {
  lock.synchronized {
    counter += 1;
    counter
  }
}

def decrementAndReturn(): Integer = {
  lock.synchronized {
    counter -= 1;
    counter
  }
}

def readCounter(): Integer = {
  counter
}