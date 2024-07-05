package hw7.scaladev.otus

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

trait Monad[F[_]] extends Functor[F] { self =>
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = ???

  def point[A](a: A): F[A] = ???

  def flatten[A](fa: F[F[A]]): F[A] = flatMap(fa)(identity)

  def map[A,B](ma: F[A])(f: A => B): F[B] =
    flatMap(ma)(a => point(f(a)))
}

object Monad {

  val optionMonad = new Monad[Option] {
    def point[A](a: => A): Option[A] = Some(a)
    override def flatMap[A,B](ma: Option[A])(f: A => Option[B]): Option[B] = ma match {
      case Some(a) => f(a)
      case None => None
    }
  }

  val listMonad = new Monad[List] {
    def point[A](a: => A): List[A] = List(a)
    override def flatMap[A,B](ma: List[A])(f: A => List[B]): List[B] = ma match {
      case Nil => Nil
      case head::tail => f(head) ::: flatMap(tail)(f)
    }
  }

  val futureMonad = new Monad[Future] {
    def point[A](a: => A): Future[A] = Future(a)
    override def flatMap[A,B](ma: Future[A])(f: A => Future[B]): Future[B] = ma.flatMap(f)
  }

}
