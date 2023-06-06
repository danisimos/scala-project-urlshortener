import cats.data.ReaderT
import cats.effect.IO
import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.util.Read
import io.estatico.newtype.macros.newtype
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, Schema}
import tofu.logging.derivation._

import java.time.Instant

package object domain {

  @derive(loggable, encoder, decoder)
  @newtype
  case class UrlId(value: Long)
  object UrlId {
    implicit val doobieRead: Read[UrlId] = Read[Long].map(UrlId(_))
    implicit val schema: Schema[UrlId] =
      Schema.schemaForLong.map(l => Some(UrlId(l)))(_.value)
    implicit val codec: Codec[String, UrlId, TextPlain] =
      Codec.long.map(UrlId(_))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class OriginalUrl(value: String)
  object OriginalUrl {
    implicit val doobieRead: Read[OriginalUrl] = Read[String].map(OriginalUrl(_))
    implicit val schema: Schema[OriginalUrl] =
      Schema.schemaForString.map(n => Some(OriginalUrl(n)))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class ShortUrl(value: String)
  object ShortUrl {
    implicit val doobieRead: Read[ShortUrl] = Read[String].map(ShortUrl(_))
    implicit val schema: Schema[ShortUrl] =
      Schema.schemaForString.map(n => Some(ShortUrl(n)))(_.value)
    implicit val codec: Codec[String, ShortUrl, TextPlain] =
      Codec.string.map(ShortUrl(_))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class ReachableUrl(value: Boolean)
  object ReachableUrl {
    implicit val doobieRead: Read[ReachableUrl] = Read[Boolean].map(ReachableUrl(_))
    implicit val schema: Schema[ReachableUrl] =
      Schema.schemaForBoolean.map(n => Some(ReachableUrl(n)))(_.value)
    implicit val codec: Codec[String, ReachableUrl, TextPlain] =
      Codec.boolean.map(ReachableUrl(_))(_.value)
  }

  type IOWithRequestContext[A] = ReaderT[IO, RequestContext, A]
}
