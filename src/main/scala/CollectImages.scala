import com.twitter.util.Future

def fetchUrl(url: String): Future[Array[Byte]] = ???
def findImageUrls(bytes: Array[Byte]): Seq[String] = ???

val url = "https://www.google.com"

// Sequential Composition
val f: Future[Array[Byte]] = 
    fetchUrl(url).flatMap { bytes =>
        val images = findImageUrls(bytes)
        if (images.isEmpty)
            Future.exception(new Exception("no image"))
        else
            fetchUrl(images.head)
}

f.onSuccess { image =>
    println("Found image of size " + image.size)
}

// Concurrent Composition
val collected: Future[Seq[Array[Byte]]] =
    fetchUrl(url).flatMap { bytes =>
        val fetches = findImageUrls(bytes).map { url => fetchUrl(url) }
        Future.collect(fetches)
}

// Parallel Composition
val numFollowers: Future[Int] = ???
val profileImageURL: Future[String] = ???
val followersYouKnow: Future[Seq[User]] = ???
val constructUserProfile: (Int, String) => UserProfile

// Future#join
val userProfileData: Future[(Int, String)] = numFollowers.join(profileImageURL)

// Future.join
val userProfileData: Future[(Int, String, Seq[User])] = Future.join(numFollowers, profileImageURL, followersYouKnow)

// Future#joinWith
val userProfile: Future[UserProfile] = numFollowers.joinWith(profileImageURL)(constructUserProfile)

// Future.join Seq
val profileDataIsReady: Future[Unit] = Future.join(Seq(numFollowers, profileImageUrl, followersYouKnow))