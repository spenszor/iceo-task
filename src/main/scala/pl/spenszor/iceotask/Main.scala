package pl.spenszor.iceotask

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.comcast.ip4s.IpLiteralSyntax
import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AmqpMessage.stringEncoder
import dev.profunktor.fs2rabbit.model.{ExchangeName, ExchangeType, QueueName, RoutingKey}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import pl.spenszor.iceotask.WebSocketRoutes.{csvReaderRoute, swaggerRoutes}

import scala.concurrent.duration._

object Main extends IOApp {


  private val config: Fs2RabbitConfig = Fs2RabbitConfig(
    host = "rabbitmq",
    port = 5672,
    virtualHost = "/",
    connectionTimeout = 3.minutes,
    ssl = false,
    username = Some("guest"),
    password = Some("guest"),
    requeueOnNack = false,
    requeueOnReject = false,
    internalQueueSize = Some(500)
  )


  private val exchangeName = ExchangeName("testEX")
  private val routingKey = RoutingKey("testRK")
  private val queueName = QueueName("testQueue")

  private def program: IO[ExitCode] = {
    RabbitClient.default[IO](config).resource.use {
      rabbitClient =>
        rabbitClient.createConnectionChannel.use {
          implicit channel =>
            for {
              _ <- rabbitClient.declareQueue(DeclarationQueueConfig.default(queueName))
              _ <- rabbitClient.declareExchange(exchangeName, ExchangeType.Topic)
              _ <- rabbitClient.bindQueue(queueName, exchangeName, routingKey)
              publisher <- rabbitClient.createPublisher[String](exchangeName, routingKey)
              csvReader = new CsvReader(publisher)
              exitCode <-
                EmberServerBuilder.default[IO]
                  .withHost(ipv4"0.0.0.0")
                  .withPort(port"8080")
                  .withHttpWebSocketApp(wsb => Router("/" -> (csvReaderRoute(csvReader)(wsb) <+> swaggerRoutes)).orNotFound)
                  .build
                  .use(_ => IO.never)
                  .as(ExitCode.Success)
            } yield exitCode
        }
    }
  }
  def run(args: List[String]): IO[ExitCode] =
    program.as(ExitCode.Success)
}
