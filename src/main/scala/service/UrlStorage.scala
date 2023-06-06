package service

import cats.syntax.applicativeError._
import cats.syntax.either._
import dao.UrlSql
import domain._
import domain.errors._
import doobie._
import doobie.implicits._
import tofu.logging.Logging

import java.time.Instant
import scala.io.Source
import scala.util.{Failure, Try}

trait UrlStorage {
  def list: IOWithRequestContext[Either[InternalError, List[Url]]]
  def findById(
      id: UrlId
  ): IOWithRequestContext[Either[InternalError, Option[Url]]]
  def removeById(id: UrlId): IOWithRequestContext[Either[AppError, Unit]]
  def create(url: OriginalUrl): IOWithRequestContext[Either[AppError, Url]]
  def getOriginalByShort(short: ShortUrl): IOWithRequestContext[Either[UrlNotFoundByShort, Option[Url]]]
}

object UrlStorage {
  private final class Impl(
      sql: UrlSql,
      transactor: Transactor[IOWithRequestContext]
  ) extends UrlStorage {
    override def list: IOWithRequestContext[Either[InternalError, List[Url]]] =
      sql.listAll.transact(transactor).attempt.map(_.leftMap(InternalError(_)))

    override def findById(
        id: UrlId
    ): IOWithRequestContext[Either[InternalError, Option[Url]]] = {
      sql
        .findById(id)
        .transact(transactor)
        .attempt
        .map(_.leftMap(InternalError))
    }

    override def removeById(
        id: UrlId
    ): IOWithRequestContext[Either[AppError, Unit]] =
      sql.removeById(id).transact(transactor).attempt.map {
        case Left(th)           => InternalError(th).asLeft[Unit]
        case Right(Left(error)) => error.asLeft[Unit]
        case _                  => ().asRight[AppError]
      }

    override def create(
        url: OriginalUrl
    ): IOWithRequestContext[Either[AppError, Url]] = {
      val reachableUrl = Try[Int] {
        Source.fromURL(url.value).reader().read()
      } match {
        case Failure(exception) => false
        case _                  => true
      }
      val createUrl: CreateUrl = CreateUrl(
        url,
        ShortUrl(""),
        ReachableUrl(reachableUrl)
      )
      sql
        .create(createUrl)
        .transact(transactor)
        .attempt
        .map(_.leftMap(InternalError(_)))
    }

    override def getOriginalByShort(
                           short: ShortUrl
                         ): IOWithRequestContext[Either[UrlNotFoundByShort, Option[Url]]] = {
      sql
        .getOriginalByShort(short)
        .transact(transactor)
        .attempt
        .map(_.leftMap(UrlNotFoundByShort))
    }
  }

  private final class LoggingImpl(storage: UrlStorage)(implicit
      logging: Logging[IOWithRequestContext]
  ) extends UrlStorage {

    private def surroundWithLogs[Error, Res](
        inputLog: String
    )(errorOutputLog: Error => (String, Option[Throwable]))(
        successOutputLog: Res => String
    )(
        io: IOWithRequestContext[Either[Error, Res]]
    ): IOWithRequestContext[Either[Error, Res]] =
      for {
        _ <- logging.info(inputLog)
        res <- io
        _ <- res match {
          case Left(error) => {
            val (msg, cause) = errorOutputLog(error)
            cause.fold(logging.error(msg))(cause => logging.error(msg, cause))
          }
          case Right(result) => logging.info(successOutputLog(result))
        }
      } yield res

    override def list: IOWithRequestContext[Either[InternalError, List[Url]]] =
      surroundWithLogs[InternalError, List[Url]]("Getting all urls") { error =>
        (s"Error while getting all urls: ${error.message}", error.cause)
      } { result =>
        s"All urls: ${result.mkString}"
      }(storage.list)

    override def findById(
        id: UrlId
    ): IOWithRequestContext[Either[InternalError, Option[Url]]] =
      surroundWithLogs[InternalError, Option[Url]](
        s"Getting url by id ${id.value}"
      ) { error =>
        (s"Error while getting url: ${error.message}\n", error.cause)
      } { result =>
        s"Found url: ${result.toString}"
      }(storage.findById(id))

    override def removeById(
        id: UrlId
    ): IOWithRequestContext[Either[AppError, Unit]] =
      surroundWithLogs[AppError, Unit](s"Removing url by id ${id.value}") {
        error => (s"Error while removing url: ${error.message}", error.cause)
      } { _ =>
        s"Removed url with id ${id.value}"
      }(storage.removeById(id))

    override def create(
        url: OriginalUrl
    ): IOWithRequestContext[Either[AppError, Url]] =
      surroundWithLogs[AppError, Url](s"Creating url with params $url") {
        error => (s"Error while creating url: ${error.message}", error.cause)
      } { url =>
        s"Created url $url"
      }(storage.create(url))

    override def getOriginalByShort(
                         short: ShortUrl
                       ): IOWithRequestContext[Either[UrlNotFoundByShort, Option[Url]]] =
      surroundWithLogs[UrlNotFoundByShort, Option[Url]](s"Finding url with params $short") {
        error => (s"Error while finding url: ${error.message}", error.cause)
      } { url =>
        s"Created url $url"
      }(storage.getOriginalByShort(short))

  }

  def make(
      sql: UrlSql,
      transactor: Transactor[IOWithRequestContext]
  ): UrlStorage = {
    implicit val logs =
      Logging.Make
        .contextual[IOWithRequestContext, RequestContext]
        .forService[UrlStorage]
    val storage = new Impl(sql, transactor)
    new LoggingImpl(storage)
  }
}
