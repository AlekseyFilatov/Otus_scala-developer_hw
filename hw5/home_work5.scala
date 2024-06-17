package hw5.scaladev.otus

import hw5.scaladev.otus.homework_hkt_implicits.{Bindable, optBindable, tupleBindable, tuplef}

import scala.language.implicitConversions


object homework_hkt_implicits {

  trait Bindable[F[_], A] {
    def map[B](f: A => B): F[B]

    def flatMap[B](f: A => F[B]): F[B]

  } // HKT


  object Bindable{

    def tupleFnc[F[_], A, B](fa: Option[A], fb: Option[B]) :Option[(A,B)] =
      fa.flatMap(a => fb.map(b => (a, b)))

    def tupleFnc[F[_], A, B](fa: Option[List[A]], fb: Option[List[B]]) :List[(A,B)] =
      getOption(fa).flatMap(a => getOption(fb).map(b => (a, b)))

    def tupleFnc[F[_], A, B](fa: Bindable[F, A], fb: Bindable[F, B]): F[(A, B)] = {
      fa.flatMap(a => fb.map(b => (a, b)))
    }

    implicit def OptToOpt[F[_], T](s: F[T]): Option[F[T]] = Some(s)

    implicit def getOption[A](intr : Option[A]): A = intr match {
      case Some(value) => value
      case None => throw new Exception(s"not convert $intr")
    }

    implicit def optionToBindable[A](opt: Option[A]): Bindable[Option, A] = {
      val implBindOpt: Bindable[Option, A] = new Bindable[Option, A] {
        override def map[B](f: A => B): Option[B] = opt.map(f)

        override def flatMap[B](f: A => Option[B]): Option[B] = opt.flatMap(f)

      }
      implBindOpt
    }

    implicit def listToBindable[A](list: List[A]): Bindable[List, A] = {
      val implBindList: Bindable[List, A] = new Bindable[List, A] {
        override def map[B](f: A => B): List[B] = list.map(f)

        override def flatMap[B](f: A => List[B]): List[B] = list.flatMap(f)

      }
      implBindList
    }

    def tupleFBindable[F[_], A, B](fa: Option[Bindable[F, A]], fb: Option[Bindable[F, B]]): F[(A, B)] = {
      Bindable.tupleFnc(fa.get, fb.get).asInstanceOf[F[(A, B)]]
    }

    def tupleBindable[F[_], A, B](fa: Bindable[F, A], fb: Bindable[F, B]): F[(A, B)] = {
      fa.flatMap(a => fb.map(b => (a, b)))
    }

    def applyOpt[A](implicit ev: Bindable[Option, A]): Bindable[Option, A] = {
      ev
    }

  }


  def tuplef[F[_], A, B](fa: F[A], fb: F[B])
  : F[(A, B)]
  = {
    /*не знаю как заменить приведение типа asInstanceOf - постоянные ошибки сигнатуры типов fa и fb*/
    fa match {
      case _ :List[A] => Bindable.tupleBindable(fa.asInstanceOf[List[A]], fb.asInstanceOf[List[B]]).asInstanceOf[F[(A, B)]]
      case _ :Option[A] => Bindable.tupleBindable(fa.asInstanceOf[Option[A]], fb.asInstanceOf[Option[B]]).asInstanceOf[F[(A, B)]]
      case _ :Bindable[F, A] => Bindable.tupleFBindable(fa.asInstanceOf[Bindable[F, A]], fb.asInstanceOf[Bindable[F, B]]).asInstanceOf[F[(A, B)]]

    }
  }

  def tupleBindable[F[_], A, B](fa: Bindable[F, A], fb: Bindable[F, B]): F[(A, B)] =
    fa.flatMap(a => fb.map(b => (a, b)))


  def optBindable[A](opt: Option[A]): Bindable[Option, A] = new Bindable[Option, A] {

    override def map[B](f: A => B): Option[B] = opt.map(f)

    override def flatMap[B](f: A => Option[B]): Option[B] = opt.flatMap(f)

  }

  def listBindable[A](lst: List[A]): Bindable[List, A] = new Bindable[List, A] {

    override def map[B](f: A => B): List[B] = lst.map(f)

    override def flatMap[B](f: A => List[B]): List[B] = lst.flatMap(f)

  }
}

object Module {
  def main(args: Array[String]): Unit = {

    val optA: Option[Int] = Some(1)
    val optB: Option[Int] = Some(2)
    val optC: Option[String] = Some("123")

    val list1 = List(1, 2, 3)
    val list2 = List(4, 5, 6)


    //lazy val r1: Option[(Int, Int)] = tupleBindable(optA, optB)
    //lazy val r2: List[(Int, Int)] = tuplef(list1, list2)

    lazy val r3: Option[(Int, Int)] = tupleBindable(optBindable(optA), optBindable(optB))
    println(r3)
    println(tupleBindable((optA),(optC)))
    println(Bindable.tupleBindable((optA),(optC)))
    lazy val r4: Option[(String, Int)] = tuplef(optC,optB)
    println(r4)
    lazy val r5: List[(Int, Int)] = tuplef(list1,list2)
    println(r5)
    println(tuplef(optBindable(optA), optBindable(optB)))
  }


}

