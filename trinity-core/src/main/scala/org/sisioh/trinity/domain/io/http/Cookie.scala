package org.sisioh.trinity.domain.io.http

import scala.language.implicitConversions

import org.jboss.netty.handler.codec.http.{Cookie => NettyCookie}

import com.twitter.finagle.http.{Cookie => FinagleCookie}

trait Cookie {

  val underlying: NettyCookie

  val name: String

  val value: String

  def withValue(value: String): this.type

  val domain: String

  def withDomain(domain: String): this.type

  val path: String

  def withPath(path: String): this.type

  val comment: String

  def withComment(comment: String): this.type

  val maxAge: Int

  def withMaxAge(maxAge: Int): this.type

  val version: Int

  def withVersion(version: Int): this.type

  val isSecure: Boolean

  def withSecure(secure: Boolean): this.type

  val isHttpOnly: Boolean

  def withHttpOnly(httpOnly: Boolean): this.type

  val commentUrl: String

  def withCommentUrl(commentUrl: String): this.type

  val isDiscard: Boolean

  def withDiscard(discard: Boolean): this.type

  val ports: Set[Int]

  def withPorts(ports: Int*): this.type

}

object Cookie {

  private[domain] implicit def toFinagle(self: Cookie): FinagleCookie =
    new FinagleCookie(toNetty(self))

  private[domain] implicit def toNetty(self: Cookie): NettyCookie =
    self match {
      case CookieImpl(underlying) => underlying
      case _ => throw new IllegalArgumentException()
    }

  private[domain] implicit def toTrinity(underlying: NettyCookie): Cookie = CookieImpl(underlying)

  def apply(underlying: NettyCookie): Cookie = new CookieImpl(underlying)

  def apply(name: String, value: String): Cookie = new CookieImpl(name, value)

}
