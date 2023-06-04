package dao

import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.either._
import domain._
import domain.errors._
import doobie._
import doobie.implicits._
import service.Base62

trait UrlSql {
  def listAll: ConnectionIO[List[Url]]
  def findById(id: UrlId): ConnectionIO[Option[Url]]
  def removeById(id: UrlId): ConnectionIO[Either[UrlNotFound, Unit]]
  def create(originalUrl: CreateUrl): ConnectionIO[Url]
  def getCount(): ConnectionIO[Option[Long]]
  def getOriginalByShort(short: ShortUrl): ConnectionIO[Option[Url]]
}

object UrlSql {
  object sqls {
    val listAllSql: Query0[Url] = sql"select * from urls".query[Url]

    def findByIdSql(id: UrlId): Query0[Url] =
      sql"select * from urls where id=${id.value}".query[Url]

    def removeByIdSql(id: UrlId): Update0 =
      sql"delete from urls where id=${id.value}".update

    def insertSql(url: CreateUrl): Update0 =
      sql"insert into urls (original_url, short_url, expired_at) values (${url.originalUrl.value}, ${url.shortUrl.value}, ${url.expiredAt.value
          .toEpochMilli()})".update

    def findByShortUrl(shortUrl: ShortUrl) =
      sql"select * from urls where short_url=${shortUrl.value}"
        .query[Url]

    def getCountSql(): Query0[Long] = sql"select count(*) from urls".query[Long]
  }

  private final class Impl extends UrlSql {
    import sqls._

    override def listAll: ConnectionIO[List[Url]] =
      listAllSql.to[List]

    override def findById(
        id: UrlId
    ): ConnectionIO[Option[Url]] =
      findByIdSql(id).option

    override def removeById(
        id: UrlId
    ): ConnectionIO[Either[UrlNotFound, Unit]] =
      removeByIdSql(id).run.map {
        case 0 => UrlNotFound(id).asLeft[Unit]
        case _ => ().asRight[UrlNotFound]
      }

    override def create(
        url: CreateUrl
    ): ConnectionIO[Url] = {

      getCountSql().option.flatMap {
        case i: Option[Int] if i.nonEmpty =>
          val short = new Base62().encode(i.get)
          val newurl = CreateUrl(url.originalUrl, ShortUrl(s"http://localhost:8080/$short"), url.expiredAt, url.reachable)
          insertSql(newurl)
            .withUniqueGeneratedKeys[UrlId]("id")
            .map(id => Url(id, newurl.originalUrl, ShortUrl(short), newurl.expiredAt, newurl.reachable))
      }
    }

    override def getCount(): ConnectionIO[Option[Long]] = {
      getCountSql().option
    }

    override def getOriginalByShort(short: ShortUrl): doobie.ConnectionIO[Option[Url]] =
      findByShortUrl(short).option
  }

  def make: UrlSql = new Impl
}
