package muffin

import cats.effect.Async
import com.comcast.ip4s.Literals.ipv4
import sttp.tapir.*
import sttp.tapir.given
import sttp.tapir.json.circe.*
import io.circe.Json
import muffin.app.AppResponse
import muffin.app.App
import muffin.app.CommandContext
import org.http4s.server.Router
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import org.http4s.ember.server.*
import zio.{Clock, Runtime, Task}
import zio.interop.catz.given
import org.http4s.implicits.given
import com.comcast.ip4s.*
import muffin.predef.{ChannelId, UserId}
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.interceptor.log.{DefaultServerLog, ServerLog}
import sttp.tapir.server.interceptor.reject.RejectHandler
import sttp.tapir.server.model.ValuedEndpointOutput

object DefaultServer {
  case class MyFailure(msg: String)

  given Schema[AppResponse] = Schema.derived

  def defaultLog[F[_]: Async] = DefaultServerLog(
    s => {
      println(s)
      Async[F].pure(())
    },
    (s, e) => {
      println(s)
      println(e)
      Async[F].pure(())
    },
    (s, e) => {
      println(s)
      println(e)
      Async[F].pure(())
    },
    (s, e) => {
      println(s)
      println(e)
      Async[F].pure(())
    },
    Async[F].pure(())
  )

  private def server[F[_]: Async](app: App[F]) =
    Router(
      "/" -> Http4sServerInterpreter[F](
        Http4sServerOptions.customiseInterceptors
          .rejectHandler(new RejectHandler[F] {
            override def apply(failure: RequestResult.Failure)(implicit
              monad: MonadError[F]
            ): F[Option[ValuedEndpointOutput[_]]] = {
              println(failure)
              ???
            }
          })
          .serverLog(defaultLog[F])
          .options
      ).toRoutes(
        endpoint.post
          .in(paths)
          .in(formBody[Map[String, String]])
          .out(jsonBody[AppResponse])
          .serverLogicSuccess { case (segments, params) =>
            app.handleCommand(
              segments.last,
              CommandContext(
                channelId = ChannelId(params("channel_id")),
                channelName = params("channel_name"),
                responseUrl = params("response_url"),
                teamDomain = params("team_domain"),
                teamId = params("team_id"),
                text = params.get("text"),
                triggerId = params("trigger_id"),
                userId = UserId(params("user_id")),
                userName = params("user_name")
              )
            )
          }
      )
    ).orNotFound

  def apply(app: App[Task])(using Runtime[Clock]) =
    EmberServerBuilder
      .default[Task]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"8080")
      .withHttpApp(server(app))
      .build

}
//.in(stringBody).out(plainBody[Int])
//
//def countCharacters(s: String): IO[Either[Unit, Int]] =
//  IO.pure(Right[Unit, Int](s.length))
//
//val countCharactersEndpoint: PublicEndpoint[String, Unit, Int, Any] =
//
//val countCharactersRoutes: HttpRoutes[IO] =
//  Http4sServerInterpreter[IO]().toRoutes(countCharactersEndpoint.serverLogic(countCharacters _))
