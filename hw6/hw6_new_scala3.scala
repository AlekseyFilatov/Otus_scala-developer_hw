package hw.scaladev.hw6

class MonadParser[T, Src](private val p: Src => (T, Src)) {
    def flatMap[M](f: T => MonadParser[M, Src]): MonadParser[M, Src] =
      MonadParser { src =>
        val (word, rest) = p(src)
        val mn = f(word)
        val res = mn.p(rest)
        res
      }


    def map[M](f: T => M): MonadParser[M, Src] =
      MonadParser { src =>
        val (word, rest) = p(src)
        (f(word), rest)
      }

    def parse(src: Src): T = p(src)._1
  }



object MonadParser {
    def apply[T, Src](f: Src => (T, Src)) = new MonadParser[T, Src](f)
  }

case class Car(year: Int, mark: String, model: String, canDrive: Boolean)

trait Parser[T] {
  def parserStr(s: String): Array[T] = ???
}

class ParserWithGivenParams(using val splitter: String) {
  def split: String = summon[String]
}

extension [T](s: String)(using spl :String)(using p: Parser[T]) {
  def convStringToSmb: Array[T] = p.parserStr(s)
}

object ParserWithGivenParams {
  def apply(splitter: String) = new ParserWithGivenParams(using splitter)
}

object TestExecutor {

    def main(args: Array[String]): Unit = {

      val str = "1997;Ford;Passat;true\n1901;Ford;T;false"
      given splitter :String = "\n"
      //val n = ParserWithGivenParams(summon[String])

      lazy val result :Array[Car] = str.convStringToSmb

      println(result.mkString("Array(", ", ", ")"))

      given p: Parser[Car] = new Parser[Car] {
        def StringField: MonadParser[String, String] = MonadParser[String, String] {
          case str =>
            val idx = str.indexOf(";")
            if (idx > -1)
              (str.substring(0, idx), str.substring(idx + 1))
            else
              (str, "")
        }

        def IntField: MonadParser[Int, String] = StringField.map(_.toInt)

        def BooleanField: MonadParser[Boolean, String] = StringField.map(_.toBoolean)
        override def parserStr(s: String): Array[Car] = {
          val splitter = summon[String]
          s.split(splitter).map {
            lazy val parserCar = for {
              year <- IntField
              mark <- StringField
              model <- StringField
              canDrive <- BooleanField
            } yield Car(year, mark, model, canDrive)
            parserCar.parse
          }
        }
      }
    }
}
