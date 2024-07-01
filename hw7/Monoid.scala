package hw7.scaladev.otus

trait Monoid[A] extends Semigroup[A] {
  def zero: A
}

object Monoid {

  implicit val intMonoid: Monoid[Int] = new Monoid[Int] {
    def zero: Int = 1

    def combine(x: Int, y: Int): Int = x * y
  }
}
