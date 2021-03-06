package org.sisioh.trinity.domain.mvc.filter

import com.twitter.finagle.Service
import com.twitter.finagle.{Filter => FinagleFilter}
import com.twitter.ostrich.stats.Stats
import com.twitter.util.Future
import org.sisioh.trinity.domain.io.http.{Request => IORequest}
import org.sisioh.trinity.domain.io.http.{Response => IOResponse}
import org.sisioh.trinity.domain.mvc.action.Action
import org.sisioh.trinity.domain.mvc.http.Request
import org.sisioh.trinity.domain.mvc.http.Response

case class GatewayFilter(action: Option[Action[Request, Response]] = None)
  extends FinagleFilter[IORequest, IOResponse, Request, Response] {

  def apply(ioRequest: IORequest, service: Service[Request, Response]): Future[IOResponse] = {
    Stats.timeFutureNanos("GatewayFilter")({
      val request = Request.fromUnderlying(ioRequest)
      service(request.withAction(action)).map {
        responseIn =>
          responseIn.underlying
      }
    })
  }

}
