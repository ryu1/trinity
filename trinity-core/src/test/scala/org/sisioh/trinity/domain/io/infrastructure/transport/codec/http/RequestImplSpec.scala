package org.sisioh.trinity.domain.io.infrastructure.transport.codec.http

import org.sisioh.trinity.domain.io.buffer.ChannelBuffers
import org.sisioh.trinity.domain.io.http.{CharsetUtil, Method, Version}
import org.specs2.mutable.Specification
import org.sisioh.trinity.domain.io.infrastructure.http.RequestImpl

class RequestImplSpec extends Specification {

  "request" should {
    "get version" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.protocolVersion must_== Version.Http11
    }
    "get uri" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.uri must_== "/index"
    }
    "get method" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.method must_== Method.Get
    }
    "contains header" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.withHeader("name", "value").containsHeader("name") must beTrue
    }
    "contains header" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.withContent(ChannelBuffers.copiedBuffer("test", CharsetUtil.UTF_8)).content.toString(CharsetUtil.UTF_8) must_== "test"
    }
    "not have headerNames" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.headerNames must haveSize(0)
    }
    "have headerNames" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.withHeader("name", "value").headerNames must haveSize(1)
    }
    "have a chunked value which false" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.isChunked must beFalse
    }
    "have a chunked value which true" in {
      val target = new RequestImpl(Method.Get, "/index", version = Version.Http11)
      target.withChunked(chunked = true).isChunked must beTrue
    }

  }

}
