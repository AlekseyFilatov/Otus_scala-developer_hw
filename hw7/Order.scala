package hw7.scaladev.otus

trait Order[A] {
  def compare(x: A, y: A): Option[Boolean]
}


