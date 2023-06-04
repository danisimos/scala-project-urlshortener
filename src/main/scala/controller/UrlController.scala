package controller

import cats.effect.IO
import cats.syntax.either._
import controller.endpoints._
import domain.errors._
import domain._
import service.{ UrlStorage}

import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future

trait UrlController {
  def listAllUrls: ServerEndpoint[Any, IO]
  def findUrlById: ServerEndpoint[Any, IO]
  def removeUrlById: ServerEndpoint[Any, IO]
  def createUrl: ServerEndpoint[Any, IO]
  def getOriginalByShort: ServerEndpoint[Any, IO]

  def all: List[ServerEndpoint[Any, IO]]
}

object UrlController {
  final private class Impl(storage: UrlStorage) extends UrlController {

    override val listAllUrls: ServerEndpoint[Any, IO] =
      listUrls.serverLogic { ctx =>
        storage.list.map(_.leftMap[AppError](identity)).run(ctx)
      }

    override val findUrlById: ServerEndpoint[Any, IO] =
      endpoints.findUrlById.serverLogic { case (id, ctx) =>
        storage.findById(id).map(_.leftMap[AppError](identity)).run(ctx)
      }

    override val removeUrlById: ServerEndpoint[Any, IO] =
      endpoints.removeUrl.serverLogic { case (id, ctx) =>
        storage.removeById(id).run(ctx)

      }

    override val createUrl: ServerEndpoint[Any, IO] =
      endpoints.createUrl.serverLogic { case (ctx, url) =>
        storage.create(url).run(ctx)
      }

    override val getOriginalByShort: ServerEndpoint[Any, IO] =
      endpoints.getOriginalByShort.serverLogic { case (short, ctx) =>
        storage.getOriginalByShort(short).map(_.leftMap[UrlNotFoundByShort](identity)).run(ctx)
      }

    override val all: List[ServerEndpoint[Any, IO]] = List(
      listAllUrls,
      findUrlById,
      removeUrlById,
      createUrl,
      getOriginalByShort
    )
  }

  def make(storage: UrlStorage): UrlController = new Impl(storage)
}
