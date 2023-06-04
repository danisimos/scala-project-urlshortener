import cats.data.ReaderT
import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s._
import config.AppConfig
import controller.UrlController
import controller.endpoints._
import dao.{ UrlSql}
import domain.{IOWithRequestContext, RequestContext}
import doobie.util.transactor.Transactor
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Router
import service.{ UrlStorage}
import sttp.tapir.AnyEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tofu.logging.Logging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  private val mainLogs =
    Logging.Make.plain[IO].byName("Main")

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      _ <- Resource.eval(mainLogs.info("Starting UrlShortener service..."))
      config <- Resource.eval(AppConfig.load)
      transactor = Transactor
        .fromDriverManager[IO](
          config.db.driver,
          config.db.url,
          config.db.user,
          config.db.password
        )
        .mapK[IOWithRequestContext](ReaderT.liftK[IO, RequestContext])
      sql = UrlSql.make
      storage = UrlStorage.make(sql, transactor)
      controller = UrlController.make(storage)
       myEndpoints: List[AnyEndpoint] = List(
         listUrls, removeUrl, createUrl, findUrlById, getOriginalByShort
       )

      swaggerEndpoints: List[ServerEndpoint[Any, IO]] = SwaggerInterpreter().fromEndpoints[IO](myEndpoints, "Scala URL Shortener", "0.1")
      routesSwagger = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)
      routes = Http4sServerInterpreter[IO]().toRoutes(controller.all)
      httpApp = Router("/" -> routes, "/" -> routesSwagger).orNotFound

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(
          Ipv4Address.fromString(config.server.host).getOrElse(ipv4"0.0.0.0")
        )
        .withPort(Port.fromInt(config.server.port).getOrElse(port"8080"))
        .withHttpApp(httpApp)
        .build
    } yield ()).useForever.as(ExitCode.Success)
}
