package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation._

@derive(loggable, encoder, decoder)
final case class CreateUrl(originalUrl: OriginalUrl, shortUrl: ShortUrl, expiredAt: ExpiredAt, reachable: ReachableUrl)

@derive(loggable, encoder, decoder)
final case class Url(id: UrlId, originalUrl: OriginalUrl, shortUrl: ShortUrl, expiredAt: ExpiredAt)
object Url {
  implicit val schema: Schema[Url] = Schema.derived
}
