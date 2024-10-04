package code.model

import zio.http._

sealed trait ResponseError { self =>
  import ResponseError._
  override def toString: String =
    self match {
      case CountryApiUnreachable(error)     => s"API unreachable: ${error.getMessage}"
      case UnexpectedResponse(status, body) => s"Response error. Status: $status, body: $body"
      case ResponseExtraction(error)        => s"Response extraction error: ${error.getMessage}"
      case ResponseParsing(message)         => s"Response parsing error: $message"
    }
}

object ResponseError {
  final case class CountryApiUnreachable(error: Throwable)          extends ResponseError
  final case class UnexpectedResponse(status: Status, body: String) extends ResponseError
  final case class ResponseExtraction(error: Throwable)             extends ResponseError
  final case class ResponseParsing(message: String)                 extends ResponseError
}
