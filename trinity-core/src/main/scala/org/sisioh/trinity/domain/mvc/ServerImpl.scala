package org.sisioh.trinity.domain.mvc

import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.builder.{Server => FinagleServer}
import com.twitter.finagle.http.{Http, RichHttp}
import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finagle.http.{Response => FinagleResponse}
import com.twitter.finagle.tracing.{NullTracer, Tracer}
import com.twitter.finagle.{Filter => FinagleFilter, Service}
import org.sisioh.dddbase.core.lifecycle.sync.SyncEntityIOContext
import org.sisioh.scala.toolbox.LoggingEx
import org.sisioh.trinity.domain.io.FinagleToIOFilter
import org.sisioh.trinity.infrastructure.util.DurationConverters._
import org.sisioh.trinity.infrastructure.util.FutureConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent._
import com.twitter.ostrich.admin.RuntimeEnvironment

private[mvc]
class ServerImpl
(serverConfig: ServerConfig,
 actionOpt: Option[Action[Request, Response]],
 filterOpt: Option[Filter[Request, Response, Request, Response]] = None,
 globalSettingsOpt: Option[GlobalSettings[Request, Response]])
  extends Server with LoggingEx {

  implicit val ctx = SyncEntityIOContext
  private var server: FinagleServer = _

  private val finagleFilterBuffers = new ListBuffer[FinagleFilter[Request, Response, Request, Response]]()

  protected def createTracer: Tracer = NullTracer

  protected def createRuntimeEnviroment: RuntimeEnvironment = new RuntimeEnvironment(this)

  filterOpt.foreach(registerFilter)

  def registerFilters(filters: Seq[Filter[Request, Response, Request, Response]])(implicit executor: ExecutionContext) {
    registerFinagleFilters(filters.map {
      Filter toFinagleFilter _
    })
  }

  def registerFilter(filter: Filter[Request, Response, Request, Response])(implicit executor: ExecutionContext) {
    registerFinagleFilter(Filter.toFinagleFilter(filter))
  }

  protected def registerFinagleFilters(filters: Seq[FinagleFilter[Request, Response, Request, Response]]) {
    finagleFilterBuffers.appendAll(filters)
  }

  protected def registerFinagleFilter(filter: FinagleFilter[Request, Response, Request, Response]) {
    finagleFilterBuffers.append(filter)
  }

  protected def applyFinagleFilters(baseService: Service[Request, Response]) = {
    finagleFilterBuffers.foldRight(baseService) {
      (b, a) =>
        b andThen a
    }
  }

  protected def createCodec = {
    import com.twitter.conversions.storage._
    val http = Http()
    serverConfig.maxRequestSizeOpt.foreach {
      v =>
        http.maxRequestSize(v.megabytes)
    }
    serverConfig.maxResponseSizeOpt.foreach {
      v =>
        http.maxResponseSize(v.megabytes)
    }
    RichHttp[FinagleRequest](http)
  }

  def start()(implicit executor: ExecutionContext): Future[Unit] = future {
    val actionExecuteService = ActionExecuteService(globalSettingsOpt)
    val service: Service[FinagleRequest, FinagleResponse] =
      FinagleToIOFilter() andThen
        GatewayFilter(actionOpt) andThen
        applyFinagleFilters(actionExecuteService)

    server = ServerBuilder()
      .codec(createCodec)
      .bindTo(serverConfig.bindAddressOpt.getOrElse(Server.defaultBindAddress))
      .tracer(createTracer)
      .name(serverConfig.nameOpt.getOrElse(Server.defaultName))
      .build(service)
  }

  def stop()(implicit executor: ExecutionContext): Future[Unit] = {
    val awaitDuration = serverConfig.awaitDurationOpt.getOrElse(Server.defaultAwaitDuration).toTwitter
    server.close(awaitDuration).toScala
  }

}
