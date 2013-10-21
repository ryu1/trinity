package org.sisioh.trinity.domain.mvc.http

import org.jboss.netty.handler.codec.http.multipart.{MixedFileUpload, HttpPostRequestDecoder}
import org.sisioh.trinity.domain.io.buffer.ChannelBuffer.toNetty
import org.sisioh.trinity.domain.io.http.Method
import org.sisioh.trinity.domain.io.http.Version
import org.sisioh.trinity.domain.io.http.{Request => IORequest}
import org.sisioh.trinity.domain.io.infrastructure.http.AbstractRequestProxy
import org.sisioh.trinity.domain.mvc.GlobalSettings
import org.sisioh.trinity.domain.mvc.action.Action
import scala.collection.JavaConverters._
import scala.util.Try

private[http]
class RequestImpl
(override val underlying: IORequest,
 val actionOpt: Option[Action[Request, Response]],
 val routeParams: Map[String, String],
 val globalSettingsOpt: Option[GlobalSettings[Request, Response]],
 val errorOpt: Option[Throwable])
  extends AbstractRequestProxy(underlying) with Request {

  val toUnderlyingAsFinagle = underlying.toUnderlyingAsFinagle

  protected def createInstance(message: this.type): this.type =
    new RequestImpl(
      message.underlying,
      message.actionOpt,
      message.routeParams,
      message.globalSettingsOpt,
      message.errorOpt
    ).asInstanceOf[this.type]

  def this(method: Method.Value,
           uri: String,
           actionOpt: Option[Action[Request, Response]],
           routeParams: Map[String, String],
           globalSettingsOpt: Option[GlobalSettings[Request, Response]],
           errorOpt: Option[Throwable],
           httpVersion: Version.Value = Version.Http11) =
    this(IORequest(method, uri, httpVersion), actionOpt, routeParams, globalSettingsOpt, errorOpt)

  def multiParams: Try[Map[String, MultiPartItem]] = Try {
    if (method == Method.Post) {
      content.markReaderIndex()
      val httpPostRequestDecoder = new HttpPostRequestDecoder(toUnderlyingAsFinagle)
      val m = if (httpPostRequestDecoder.isMultipart) {
        httpPostRequestDecoder.getBodyHttpDatas.asScala.map {
          data =>
            data.getName -> MultiPartItem(data.asInstanceOf[MixedFileUpload])
        }.toMap
      } else Map.empty[String, MultiPartItem]
      content.resetReaderIndex()
      m
    } else Map.empty[String, MultiPartItem]
  }

  def withActionOpt(actionOpt: Option[Action[Request, Response]]): this.type =
    new RequestImpl(underlying, actionOpt, routeParams, globalSettingsOpt, errorOpt).asInstanceOf[this.type]

  def withRouteParams(routeParams: Map[String, String]): this.type =
    new RequestImpl(underlying, actionOpt, routeParams, globalSettingsOpt, errorOpt).asInstanceOf[this.type]

  def withError(error: Throwable): this.type =
    new RequestImpl(underlying, actionOpt, routeParams, globalSettingsOpt, Some(error)).asInstanceOf[this.type]

}
