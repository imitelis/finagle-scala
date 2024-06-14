// Clients and Servers
import com.twitter.finagle.Http

val client = Http.client
  .withLabel("my-http-client")
  .withTransport.verbose
  .newService("localhost:10000,localhost:10001")

import com.twitter.finagle.Http
import com.twitter.transport.Transport

val client = Http.client
  .configured(Transport.Options(noDelay = false, reuseAddr = false))
  .newService("localhost:10000,localhost:10001")

// Filter with Toggle
package com.example.service

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.toggle.{Toggle, ToggleMap}
import com.twitter.finagle.util.Rng

class ExampleFilter(
    toggleMap: ToggleMap,
    newBackend: Service[Request, Response])
  extends SimpleFilter[Request, Response] {

  private[this] val useNewBackend: Toggle = toggleMap("com.example.service.UseNewBackend")

  def apply(req: Request, service: Service[Request, Response]): Future[Response] = {
    if (useNewBackend(Rng.threadLocal.nextInt()))
      newBackend(req)
    else
      service(req)
  }
}

// Toggle Flags
import com.twitter.finagle.toggle.flag

flag.overrides.let("your.toggle.id.here", fractionToUse) {
  // code that uses the flag in this block will have the
  // flag's fraction set to `fractionToUse`.
}

// Tunables
package com.example.service

import com.twitter.finagle.Http
import com.twitter.finagle.tunable.StandardTunableMap
import com.twitter.util.Duration
import com.twitter.util.tunable.{Tunable, TunableMap}

val clientId = "exampleClient"
val timeoutTunableId = "com.example.service.Timeout"

val tunables: TunableMap = StandardTunableMap(clientId)
val timeoutTunable: Tunable[Duration] =
  tunables(TunableMap.Key[Duration](timeoutTunableId))

val client = Http.client
  .withLabel(clientId)
  .withRequestTimeout(timeoutTunable)
  .newService("localhost:10000")

// In-Memory
val map = TunableMap.newMutable(source)