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
package org.sisioh.trinity.domain.mvc.routing

import org.sisioh.dddbase.core.lifecycle.sync.SyncEntityIOContext
import org.sisioh.scala.toolbox.LoggingEx
import org.sisioh.trinity.domain.mvc._
import org.sisioh.trinity.domain.mvc.action.{NotFoundHandleAction, InternalServerErrorAction, Action}
import org.sisioh.trinity.domain.mvc.filter.Filter
import org.sisioh.trinity.domain.mvc.http.{Response, Request}
import org.sisioh.trinity.domain.mvc.routing.pathpattern.{SinatraPathPatternParser, PathPatternParser}
import scala.concurrent.{Future, ExecutionContext}

/**
 * ルーティング用フィルター。
 *
 * @param routeRepository [[org.sisioh.trinity.domain.mvc.routing.RouteRepository]]
 * @param globalSettings [[org.sisioh.trinity.domain.mvc.GlobalSettings]]
 * @param executor [[org.sisioh.trinity.domain.mvc.GlobalSettings]]
 */
case class RoutingFilter
(routeRepository: RouteRepository,
 globalSettings: Option[GlobalSettings[Request, Response]])
(implicit executor: ExecutionContext)
  extends Filter[Request, Response, Request, Response] with LoggingEx {

  /**
   * アクションが見つからない場合のリカバリを行うためのハンドラ。
   *
   * @return `Future`にラップされた[[com.twitter.finagle.http.Request]]
   */
  protected def notFoundHandler: Option[Action[Request, Response]] = {
    globalSettings.flatMap {
      _.notFound
    }.orElse(Some(NotFoundHandleAction))
  }

  /**
   * エラー発生時のリカバリを行うためのハンドラ。
   *
   * @return `Future`にラップされた[[com.twitter.finagle.http.Request]]
   */
  protected def errorHandler(request: Request, throwable: Throwable): Future[Response] = {
    val newRequest = request.withError(throwable)
    globalSettings.map {
      _.error.map(_(newRequest)).
        getOrElse(InternalServerErrorAction(newRequest))
    }.getOrElse {
      InternalServerErrorAction(request)
    }
  }

  protected def getActionWithRouteParams(request: Request): Option[(Action[Request, Response], Map[String, String])] = {
    implicit val ctx = SyncEntityIOContext
    routeRepository.find {
      case Route(RouteId(m, pattern), _) =>
        val routeParamsOpt = pattern(request.path.split('?').head)
        if (routeParamsOpt.isDefined && m == request.method)
          true
        else
          false
    }.flatMap {
      case Route(RouteId(_, pattern), action) =>
        val routeParamsOpt = pattern(request.path.split('?').head)
        routeParamsOpt.map {
          routeParams =>
            (action, request.routeParams ++ routeParams)
        }
    }
  }

  def apply(request: Request, service: Action[Request, Response]): Future[Response] = {
    if (request.action.isDefined) {
      service(request)
    } else {
      val actionWithRouteParams = getActionWithRouteParams(request)
      val action = actionWithRouteParams.map(_._1).orElse(notFoundHandler)
      val routeParams = actionWithRouteParams.map(_._2).getOrElse(Map.empty)
      service(request.withAction(action).withRouteParams(routeParams))
    }
  }

}

/**
 * コンパニオンオブジェクト。
 */
object RoutingFilter extends LoggingEx {

  private implicit val ctx = SyncEntityIOContext

  def createForControllers(controllers: RouteDefHolder*)
                          (implicit executor: ExecutionContext,
                           globalSettings: Option[GlobalSettings[Request, Response]] = None,
                           pathPatternParser: PathPatternParser = SinatraPathPatternParser()): RoutingFilter = withDebugScope("createForControllers") {
    createForActions {
      pathPatternParser =>
        val result = controllers.flatMap(_.getRouteDefs)
        debug(s"result = $result")
        result
    }
  }

  def createForActions(routeDefs: (PathPatternParser) => Seq[RouteDef])
                      (implicit executor: ExecutionContext,
                       globalSettings: Option[GlobalSettings[Request, Response]] = None,
                       pathPatternParser: PathPatternParser = SinatraPathPatternParser()): RoutingFilter = {
    val routeRepository = RouteRepository.ofMemory
    routeDefs(pathPatternParser).foreach {
      case RouteDef(method, pathPattern, action) =>
        routeRepository.store(Route(method, pathPattern, action))
    }
    RoutingFilter(routeRepository, globalSettings)
  }

}
