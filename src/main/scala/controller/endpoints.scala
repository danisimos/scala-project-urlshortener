package controller

import domain._
import domain.errors._
import sttp.tapir._
import sttp.tapir.json.circe._

object endpoints {
  val listUrls: PublicEndpoint[RequestContext, AppError, List[Url], Any] =
    endpoint.get
      .in("urls")
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[Url]])

  val findUrlById
      : PublicEndpoint[(UrlId, RequestContext), AppError, Option[Url], Any] =
    endpoint.get
      .in("urls" / path[UrlId])
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Option[Url]])

  val removeUrl: PublicEndpoint[(UrlId, RequestContext), AppError, Unit, Any] =
    endpoint.delete
      .in("urls" / path[UrlId])
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[AppError])

  val createUrl
      : PublicEndpoint[(RequestContext, OriginalUrl), AppError, Url, Any] =
    endpoint.post
      .in("urls")
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[OriginalUrl])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Url])

  val getOriginalByShort
      : PublicEndpoint[(ShortUrl, RequestContext), AppError, Option[Url], Any] =
    endpoint.get
      .in(path[ShortUrl])
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Option[Url]])

}
