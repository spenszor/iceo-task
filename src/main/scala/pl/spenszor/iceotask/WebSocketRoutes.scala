package pl.spenszor.iceotask

import cats.effect._

import fs2._
import org.http4s.HttpRoutes

import org.http4s.server.websocket.WebSocketBuilder2
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter


object WebSocketRoutes {

  private val websocketEndpoint: PublicEndpoint[Unit, Unit, Pipe[IO, Int, Int], Fs2Streams[IO] with WebSockets] =
    endpoint.get
      .in("count")
      .out(webSocketBody[Int, CodecFormat.TextPlain, Int, CodecFormat.TextPlain](Fs2Streams[IO]))

  private def websocketPipe(reader: CsvReader): Pipe[IO, Int, Int] = { in =>
    in.flatMap(reader.partitionStream)
  }

  private val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[IO](List(websocketEndpoint), "Iceo Task", "1.0")

  def csvReaderRoute(reader: CsvReader): WebSocketBuilder2[IO] => HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toWebSocketRoutes(websocketEndpoint.serverLogicSuccess[IO](_ => IO(websocketPipe(reader))))

  val swaggerRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

}
