val semaphore1, semaphore2 = new AsyncSemaphore(1)
// The semaphores have already been taken:
val permitForSemaphore1 = await(semaphore1.acquire())
val permitForSemaphore2 = await(semaphore2.acquire())

// The semaphores have had continuations attached as follows:
semaphore1.acquire().flatMap { permit =>
  val otherWaiters = semaphore2.numWaiters // synchronizing method
  permit.release()
  otherWaiters
}

semaphore2.acquire().flatMap { permit =>
  val otherWaiters = semaphore1.numWaiters // synchronizing method
  permit.release()
  otherWaiters
}

val threadOne = new Thread {
  override def run() {
    permitForSemaphore1.release()
  }
}

val threadTwo = new Thread {
  override def run() {
    permitForSemaphore2.release()
  }
}

threadOne.start
threadTwo.start

// old implementation
// def release(): Unit = self.synchronized {
//  val next = waitq.pollFirst()
//  if (next != null) next.setValue(this) // <- nogo: still synchronized
//  else availablePermits += 1
// }

// new implementation

// Here we define a specific lock object, rather than use the `self` of `this`
// reference. It is synonymous with the internal Queue as that is what is
// driving our need for synchronization, but using this special-purpose reference
// gives us an opportunity in the future to refactor more easily.
private[this] final def lock: Object = waitq

@tailrec def release(): Unit = {
  // we pass the Promise outside of the lock
  val waiter = lock.synchronized {
    val next = waitq.pollFirst()
    if (next == null) {
      availablePermits += 1
    }
    next
  }

  if (waiter != null) {
    // since we are no longer synchronized with the interrupt handler
    // we leverage the atomic state of the Promise to do the right
    // thing if we race.
    if (!waiter.updateIfEmpty(Return(this))) {
      release()
    }
  }
}