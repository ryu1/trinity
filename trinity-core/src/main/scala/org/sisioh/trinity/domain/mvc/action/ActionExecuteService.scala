/*
 * Copyright 2013 Sisioh Project and others. (http://sisioh.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sisioh.trinity.domain.mvc.action

import com.twitter.finagle.Service
import com.twitter.util.Future
import org.sisioh.scala.toolbox.LoggingEx
import org.sisioh.trinity.domain.mvc._
import org.sisioh.trinity.domain.mvc.http.{Response, Request}
import org.sisioh.trinity.util.FutureConverters._
import scala.concurrent.ExecutionContext

/**
 * リクエストに関連づくアクションを実行するためのサービス。
 *
 * @param globalSettings [[org.sisioh.trinity.domain.mvc.GlobalSettings]]
 * @param executor [[scala.concurrent.ExecutionContext]]
 */
case class ActionExecuteService
(globalSettings: Option[GlobalSettings[Request, Response]] = None)
(implicit executor: ExecutionContext)
  extends Service[Request, Response] with LoggingEx {

  /**
   * アクションが見つからない場合のリカバリを行うためのハンドラ。
   *
   * @return `Future`にラップされた[[com.twitter.finagle.http.Request]]
   */
  protected def notFoundHandler: Action[Request, Response] = {
    globalSettings.flatMap {
      _.notFound
    }.getOrElse(NotFoundHandleAction)
  }

  def apply(request: Request): Future[Response] = {
    request.execute(notFoundHandler).toTwitter
  }

}
