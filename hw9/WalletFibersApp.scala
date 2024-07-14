package catsconcurrency.cats_effect_homework

import cats.effect.kernel.{Ref, Spawn}
import cats.effect.{IO, IOApp}
import cats.implicits._

import scala.collection.View.Iterate
import scala.concurrent.duration.DurationInt

// Поиграемся с кошельками на файлах и файберами.

// Нужно написать программу где инициализируются три разных кошелька и для каждого из них работает фоновый процесс,
// который регулярно пополняет кошелек на 100 рублей раз в определенный промежуток времени. Промежуток надо сделать разный, чтобы легче было наблюдать разницу.
// Для определенности: первый кошелек пополняем раз в 100ms, второй каждые 500ms и третий каждые 2000ms.
// Помимо этих трёх фоновых процессов (подсказка - это файберы), нужен четвертый, который раз в одну секунду будет выводить балансы всех трех кошельков в консоль.
// Основной процесс программы должен просто ждать ввода пользователя (IO.readline) и завершить программу (включая все фоновые процессы) когда ввод будет получен.
// Итого у нас 5 процессов: 3 фоновых процесса регулярного пополнения кошельков, 1 фоновый процесс регулярного вывода балансов на экран и 1 основной процесс просто ждущий ввода пользователя.

// Можно делать всё на IO, tagless final тут не нужен.

// Подсказка: чтобы сделать бесконечный цикл на IO достаточно сделать рекурсивный вызов через flatMap:
// def loop(): IO[Unit] = IO.println("hello").flatMap(_ => loop())
object WalletFibersApp extends IOApp.Simple {

  /*----пришел к выводу что для вывода общего баланса по всем картам необходима Ref - запоминает какие карты должны выводиться*/
  /*----walletIn.balance выводит IO{...} - как это побороть не знаю поэтому заменил на чтение из файлов */
  def processPupWallet(walletIn: Wallet[IO])(refBalance: Ref[IO, collection.mutable.Map[String,String]])(id: String)(sl: Int)(amount: BigDecimal): IO[Unit] =
    {IO.sleep(sl.millis) *> walletIn.topup(amount) *> refBalance.update(_.updated(id, walletIn.balance.toString))}.iterateWhile( _ => true)

  def showBalanceWallet(refBalance: Ref[IO, collection.mutable.Map[String,String]]): IO[Unit] =
    IO.sleep(1.seconds) *> {for{
    balanceMap <- refBalance.get
    } yield balanceMap.foreach{ case (x , y)  =>
      println(s"balance: ${x} - ${java.nio.file.Files.readString(java.nio.file.Paths.get(x).toAbsolutePath)}")}}
      .iterateWhile( _ => true)

  def run: IO[Unit] =
    for {
      _ <- IO.println("Press any key to stop...")
      refBalance <- Ref.of[IO, collection.mutable.Map[String, String]](collection.mutable.Map("1" -> "0", "2" -> "0", "3" -> "0"))
      wallet1 <- Wallet.fileWallet[IO]("1")
      wallet2 <- Wallet.fileWallet[IO]("2")
      wallet3 <- Wallet.fileWallet[IO]("3")
      fiber1 <- Spawn[IO].start(processPupWallet(wallet1)(refBalance)("1")(100)(100))
      fiber2 <- Spawn[IO].start(processPupWallet(wallet2)(refBalance)("2")(500)(100))
      fiber3 <- Spawn[IO].start(processPupWallet(wallet3)(refBalance)("3")(2000)(100))
      fiber4 <- Spawn[IO].start(showBalanceWallet(refBalance))
      _ <- IO.readLine
      _ <- fiber1.cancel
      _ <- fiber2.cancel
      _ <- fiber3.cancel
      _ <- fiber4.cancel
      // todo: запустить все файберы и ждать ввода от пользователя чтобы завершить работу
    } yield ()

}
