package hw.scaladev.otus

import scala.annotation.tailrec
import scala.language.{existentials, postfixOps}


object module1hw {
  def main(args: Array[String]): Unit = {


    //println(List2("a","b","c").flatMap(_.toUpperCase))
    //println(List2("a","b","c").flatMap(x => List(s"!!!$x")))



    Option2("123").printIfAny
    println(Option2("321").zip2(Option2("456")))

    println(List("1","2","3").reverse)

    val a = 1::2::3::Nil

    val x = "1"::"2"::"3"::Nil


    println(x.reverse)

    println(List2("1","2","3").mkString(";"))
    println(List2().mkString(";"))

    println(List2("5","6","7").map(x => x + 1))

    println(List2(1,2,3).filter(x => (x > 1)))

    println(List2(1,2,3,4,5,6,7).incList())
    println(List2("1","2","3","8").shoutString())
  }

  /**
   * Реализовать метод вычисления n!
   * n! = 1 * 2 * ... n
   */

  def fact(n: Int): Int = {
    var _n = 1
    var i = 2
    while (i <= n) {
      _n *= i
      i += 1
    }
    _n
  }


  def factRec(n: Int): Int =
    if (n <= 0) 1 else n * factRec(n - 1)

  def factTailRec(n: Int): Int = {

    def go(n: Int, accum: Int): Int =
      if (n <= 0) accum else go(n - 1, n * accum)

    go(n, 1)
  }

  /**
   * реализовать вычисление N числа Фибоначчи
   * F0 = 0, F1 = 1, Fn = Fn-1 + Fn - 2
   */


  object hof {

    // обертки

    def logRunningTime[A, B](f: A => B): A => B = a => {
      val start = System.currentTimeMillis()
      val result: B = f(a)
      val end = System.currentTimeMillis()
      println(s"Running time: ${end - start}")
      result
    }

    def doomy(string: String): Unit = {
      Thread.sleep(1000)
      println(string)
    }

    // изменение поведения ф-ции

    def not[A](f: A => Boolean): A => Boolean = a => !f(a)

    def isOdd(i: Int): Boolean = i % 2 > 0

    val isEven: Int => Boolean = not(isOdd)



    // изменение самой функции

    def partial[A, B, C](a: A, f: (A, B) => C): B => C = b => f(a, b)

    def partial2[A, B, C](a: A, f: (A, B) => C): B => C =
      f.curried(a)

    def sum(x: Int, y: Int): Int = x + y

    val p: Int => Int = partial(3, sum)
    p(2) // 5
    p(3) // 6
    partial(3, sum)(3) // 6


  }


  /**
   * Реализуем тип Option
   */

  class Animal

  class Dog extends Animal

  /**
   *
   * Реализовать структуру данных Option, который будет указывать на присутствие либо отсутсвие результата
   */
  // + covariance
  // - contravariance

  sealed trait Option2[+T] {

    def isEmpty2: Boolean = this match {
      case None2 => true
      case Some2(v) => false
    }
    type intOption = Option2[T] forSome { type T >: Int }
    /**
     *
     * Реализовать метод printIfAny, который будет печатать значение, если оно есть
     */
    def printIfAny(): Unit = this match {
      case Some2(v) => println(v) //v: Any => println(_)
      case None2 => ()
    }

    def get2: T = this match {
      case None2 => throw new Exception("get ob empty option")
      case Some2(v) => v
    }



    /**
     *
     * Реализовать метод zip, который будет создавать Option от пары значений из 2-х Option
     */
    def zip2[TT >: T, B](that: Option2[B]): Option2[(TT, B)] =
      (this, that) match {
        /*if (this.isEmpty2 || that.isEmpty2) None2
        else Option2((this.get2, that.get2))*/
        case (Some2(a), Some2(b)) => Option2(a, b)
        case _ => None2
      }

    /**
     *
     * Реализовать метод filter, который будет возвращать не пустой Option
     * в случае если исходный не пуст и предикат от значения = true
     */
    def filter2(p: T => Boolean): Option2[T] = this match {
      case Some2(v) if p(v) => this
      case _ => None2
    }

    def map[B](f: T => B): Option2[B] = flatMap(t => Option2(f(t)))

    def flatMap[B](f: T => Option2[B]): Option2[B] = this match {
      case Some2(v) => f(v)
      case None2 => None2
    }


  }

  // val opt1: Option[Int] = Option(0)
  // val opt2: Option[Int] = opt1.map(i => i + 1)

  case class Some2[T](v: T) extends Option2[T]

  case object None2 extends Option2[Nothing]


  object Option2 {
    def apply[T](v: T): Option2[T] =
      if (v != null) Some2(v) else None2
  }










  // }

  //object list2 {

  /**
   *
   * Реализовать односвязанный иммутабельный список List
   * Список имеет два случая:
   * Nil - пустой список
   * Cons - непустой, содержит первый элемент (голову) и хвост (оставшийся список)
   */
  sealed trait List2[+T]
  {


    /**
     * Метод cons, добавляет элемент в голову списка, для этого метода можно воспользоваться названием `::`
     *
     */
    def ::[TT >: T](el: TT): List2[TT] = new ::(el, this)


    /**
     *
     * Реализовать метод reverse который позволит заменить порядок элементов в списке на противоположный
     */
    def reverse: List2[T] = {
      @tailrec
      def rlRec(result: List2[T], list: List2[T]): List2[T] = {
        list match {
          case Nil => result
          case (x :: xs) => rlRec(x :: result, xs)
        }
      }
      rlRec(Nil, this)
    }

    /**
     * Метод mkString возвращает строковое представление списка, с учетом переданного разделителя
     *
     */
    def mkString(s: String): String = {
      val r: (String => String)  = x => s"$s$x"
      @tailrec
      def rlRec(strBuilder: StringBuilder ,list: List2[T], f: String => String): StringBuilder = {
        list match {
          case Nil => strBuilder
          case (x :: xs) => {
            rlRec(strBuilder.append(f(s"$x")), xs, f)
          }
        }
      }
      rlRec(new StringBuilder, this , r).result.drop(1)
    }

    /**
     *
     * Реализовать метод map для списка который будет применять некую ф-цию к элементам данного списка
     */
    def map[B](f: T => B): List2[B] = {
      @scala.annotation.tailrec
      def aux(k: List2[B], xs: List2[T]): List2[B] = xs match {
        case Nil => k
        case x :: xs => aux(f(x) :: k,xs)
      }
      aux(Nil, this).reverse
    }

    /*def flatMap[B](f: T => IterableOnce[B]) :List2[IterableOnce[B]] = {
      def aux(xs: List2[T]): List2[IterableOnce[B]] = xs match {
        case Nil => List2[IterableOnce[B]]()
        case x :: xs1 => for (x <- xs) yield f(x)

      }

      aux(this)
    }*/

   
   def flatMap[B](f: T => IterableOnce[B]) :List2[B] = {
      var h: ::[B] = null
      var t: ::[B] = null
      val n = scala.collection.immutable.Nil
      def aux(xs: List2[T]): List2[B] = xs match {
        case Nil => List2[B]()
        case (x :: xs1) => {
          xs.map(x => {
            val it = f(x).iterator
            while (it.hasNext) {
              var nx = new::(it.next(), Nil)
              if (t eq null) {
                h = nx
              } else {
                t.tail = nx
              }
              t = nx
            }

            if (h eq null) n.tail else {releaseFence(); h.head}
          })
        }
      }
      aux(this)
    }


   /**
     *
     * Реализовать метод filter для списка который будет фильтровать список по некому условию
     */
    def filter(p: T => Boolean): List2[T] = {
      @scala.annotation.tailrec
      def aux(k: List2[T] => List2[T], xs: List2[T]): List2[T] = xs match {
        case Nil => k(Nil)
        case x :: xs if p(x) => aux((rest: List2[T]) => k(x :: rest), xs)
        case x :: xs           => aux(k,xs)
      }
      aux(identity, this)
    }


    /**
     *
     * Написать функцию incList котрая будет принимать список Int и возвращать список,
     * где каждый элемент будет увеличен на 1
     */
      def incList(): List2[Int] = this match {
      case v: List2[Int] => {
        def aux(xs: List2[Int]): List2[Int] = xs match {
          case Nil => xs
          case x :: xs1 => for (x <- xs) yield (x + 1)
        }
        aux(v)
      }
    }

    /**
     *
     * Написать функцию shoutString котрая будет принимать список String и возвращать список,
     * где к каждому элементу будет добавлен префикс в виде '!'
     */
    def shoutString(): List2[String] = this match {
      case v: List2[String] => {
        val r: String => String = x => s"!$x"

        @scala.annotation.tailrec
        def aux(k: List2[String], xs: List2[String]): List2[String] = xs match {
          case Nil => k
          case x :: xs => aux(r(x) :: k, xs)
        }

        aux(Nil, v).reverse
      }
    }
      }


  case class ::[T](head: T, tail: List2[T]) extends List2[T]

  case object Nil extends List2[Nothing]

  object List2 {


    /**
     * Конструктор, позволяющий создать список из N - го числа аргументов
     * Для этого можно воспользоваться *
     *
     * Например вот этот метод принимает некую последовательность аргументов с типом Int и выводит их на печать
     * def printArgs(args: Int*) = args.foreach(println(_))
     */
    def apply[A](v: A*): List2[A] = if (v.isEmpty) Nil
    else ::(v.head, List2.apply(v.tail: _*))






  }
}









