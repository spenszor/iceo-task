package pl.spenszor.iceotask

import cats.effect.IO
import cats.implicits.catsSyntaxApply
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j._


class CsvReader(publisher: String => IO[Unit]) {

  private val logger = LoggerFactory[IO].getLogger

  // the splitting part in instruction was unclear, I divided it in parts of size x
  // or you expected me to create chunk when element matches n % x == 0? (so use split(_ % x == 0))?
  def partitionStream(parts: Int) = {
    if (parts <= 0) {
      fs2.Stream.eval(logger.error(s"The divisor must be positive integer, but was: $parts")) *>
        fs2.Stream.raiseError[IO](new RuntimeException(s"The divisor must be positive integer, but was: $parts"))
    } else
      Files[IO].readUtf8Lines(Path("/data.csv"))
        .map(trimComma(_).toInt % parts)
        .sliding(parts)
        .flatMap(ch => fs2.Stream.chunk(ch).foldMonoid)
        .parEvalMapUnbounded(el => publisher(el.toString) *> IO.pure(el))
    // or just .evalTap(el => publisher(el.toString))
  }

  private def trimComma(line: String) = {
    if (line.endsWith(","))
      line.dropRight(1).mkString
    else
      line
  }

}
