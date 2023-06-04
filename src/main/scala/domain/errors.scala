package domain

import cats.syntax.option._
import io.circe.{Decoder, Encoder, HCursor, Json}
import sttp.tapir.Schema

object errors {
  sealed abstract class AppError(
      val message: String,
      val cause: Option[Throwable] = None
  )
  object AppError {
    implicit val encoder: Encoder[AppError] = new Encoder[AppError] {
      override def apply(a: AppError): Json = Json.obj(
        ("message", Json.fromString(a.message))
      )
    }

    implicit val decoder: Decoder[AppError] = new Decoder[AppError] {
      override def apply(c: HCursor): Decoder.Result[AppError] =
        c.downField("message").as[String].map(MockError(_))
    }

    implicit val schema: Schema[AppError] = Schema.string[AppError]
  }

  case class UrlNotFound(id: UrlId)
    extends AppError(s"Url with id ${id.value} not found")
  case class UrlNotFoundByShort(cause0: Throwable)
    extends AppError(s"Url with short not found")
  case class UrlExpired(id: UrlId)
    extends AppError(s"Url with id ${id.value} expired")
  case class InternalError(cause0: Throwable)
      extends AppError("Internal error", cause0.some)
  case class GeneratingShortError()
    extends AppError("Generating short url error")
  case class MockError(override val message: String) extends AppError(message)
}
