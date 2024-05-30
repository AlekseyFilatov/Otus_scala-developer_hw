package hw2.scaladev.otus

import scala.util.Random

  class BallsExperiment {

   private lazy val r: Random.type = scala.util.Random
    private lazy val listBalls = List[Ballinbascketcaseclass](
      Ballinbascketcaseclass(0, 0),
      Ballinbascketcaseclass(1, 0),
      Ballinbascketcaseclass(2, 0),
      Ballinbascketcaseclass(3, 1),
      Ballinbascketcaseclass(4, 1),
      Ballinbascketcaseclass(5, 1))


    def isFirstBlackSecondWhite(): Boolean = {
      lazy val firstRand = listBalls(r.nextInt(listBalls.length - 1))
      lazy val firstExperiment = listBalls.filter( x => x.ind != firstRand.ind)
      lazy val secondRand = firstExperiment(r.nextInt(firstExperiment.length - 1))
      lazy val finalResult = (firstRand, secondRand) match {
        case (value1, value2) => ResultOfExperiments(value1, value2)
      }

      def exmplExperiment (ex :ResultOfExperiments) :Boolean = {
        if (ex.hand1.color == 0 && ex.hand2.color == 1) true else false
      }

      exmplExperiment(finalResult)
    }
  }

  case class Ballinbascketcaseclass(ind: Int, color: Int )
  case class ResultOfExperiments (hand1 :Ballinbascketcaseclass, hand2 :Ballinbascketcaseclass)

  object BallsTest {
    def main(args: Array[String]): Unit = {
      val count = 10000
      val listOfExperiments: List[BallsExperiment] = for(x <- List.range(0, count)) yield new BallsExperiment
      val countOfExperiments = listOfExperiments.map(x => x.isFirstBlackSecondWhite)
      val countOfPositiveExperiments: Float = countOfExperiments.count(_ == true)
      println(countOfPositiveExperiments / count)
    }

}
