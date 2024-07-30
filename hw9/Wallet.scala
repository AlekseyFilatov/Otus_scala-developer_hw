package catsconcurrency.cats_effect_homework

import cats.effect.{IO, Resource, Sync}
import cats.implicits._
import Wallet.{BalanceTooLow, _}

import java.nio.file.Files
import scala.collection.convert.ImplicitConversions.`iterator asScala`

// DSL управления электронным кошельком
trait Wallet[F[_]] {
  // возвращает текущий баланс
  def balance: F[BigDecimal]
  // пополняет баланс на указанную сумму
  def topup(amount: BigDecimal): F[Unit]
  // списывает указанную сумму с баланса (ошибка если средств недостаточно)
  def withdraw(amount: BigDecimal): F[Either[WalletError, Unit]]
}

// Игрушечный кошелек который сохраняет свой баланс в файл
// todo: реализовать используя java.nio.file._
// Насчёт безопасного конкуррентного доступа и производительности не заморачиваемся, делаем максимально простую рабочую имплементацию. (Подсказка - можно читать и сохранять файл на каждую операцию).
// Важно аккуратно и правильно завернуть в IO все возможные побочные эффекты.
//
// функции которые пригодятся:
// - java.nio.file.Files.write
// - java.nio.file.Files.readString
// - java.nio.file.Files.exists
// - java.nio.file.Paths.get
final class FileWallet[F[_]: Sync](id: WalletId) extends Wallet[F] {
  def balance: F[BigDecimal] = for {
    //_ <- Sync[F].delay(println(java.nio.file.Paths.get(id).toAbsolutePath))
    balance <- Sync[F].delay(java.nio.file.Files.readString(java.nio.file.Paths.get(id).toAbsolutePath))
    //_ <- Sync[F].delay(println(s"${Thread.currentThread()} inp balance: ${balance}"))
  } yield BigDecimal(balance)
  def topup(amount: BigDecimal): F[Unit] = for {
    amountPure <- Sync[F].pure(amount)
    balanceDelay <- balance
    newBalance <- Sync[F].delay((balanceDelay + amountPure).toString)
    _ <- Sync[F].delay(java.nio.file.Files.write(java.nio.file.Paths.get(id).toAbsolutePath,newBalance.getBytes(
      java.nio.charset.StandardCharsets.UTF_8),
      java.nio.file.StandardOpenOption.CREATE,
      java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))
  } yield ()

  def withdraw(amount: BigDecimal): F[Either[WalletError, Unit]] = {
    val withdrawAmaunt = for {
      amountPure <- Sync[F].pure(amount)
      balanceDelay <- balance
      newBalance <- Sync[F].delay((balanceDelay - amountPure).toString)
      _ <- Sync[F].delay(java.nio.file.Files.write(java.nio.file.Paths.get(id).toAbsolutePath,newBalance.getBytes(
        java.nio.charset.StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE,
        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))
    }  yield BigDecimal(newBalance)
    resourceOrError(id)
  }
  /*Sync[F].delay[BigDecimal](BigDecimal(newBalance))
    .ensure(new RuntimeException())(x => {
      x > 0
    }).attempt
    .map(_.leftMap(_ => BalanceTooLow).void)*/

}


object Wallet {

  // todo: реализовать конструктор
  // внимание на сигнатуру результата - инициализация кошелька имеет сайд-эффекты
  // Здесь нужно использовать обобщенную версию уже пройденного вами метода IO.delay,
  // вызывается она так: Sync[F].delay(...)
  // Тайпкласс Sync из cats-effect описывает возможность заворачивания сайд-эффектов
  def fileWallet[F[_]: Sync](id: WalletId): F[Wallet[F]] = for {
    //_ <- Sync[F].delay(println(java.nio.file.Paths.get(id).toAbsolutePath))
    amountPure <- Sync[F].pure("0")
    _ <- Sync[F].delay(java.nio.file.Files.write(java.nio.file.Paths.get(id).toAbsolutePath,amountPure.getBytes(
      java.nio.charset.StandardCharsets.UTF_8),
      java.nio.file.StandardOpenOption.CREATE,
      java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))
  } yield new FileWallet[F](id)

  type WalletId = String

  sealed trait WalletError
  case object BalanceTooLow extends WalletError
  def bufferedReader[F[_]:Sync](f: java.io.File): Resource[F, java.io.BufferedReader] =
    Resource.make {
      Sync[F].delay(new java.io.BufferedReader(new java.io.FileReader(f)))
    } { fileReader =>
      Sync[F].delay(fileReader.close()).handleErrorWith(_ => Sync[F].unit)
    }

  def resourceOrError[F[_]:Sync](fname: String): F[Either[WalletError, Unit]] =
    bufferedReader(new java.io.File(fname))
      .use(resource => Sync[F].delay(resource.lines().iterator().mkString))
      .ensure(new RuntimeException())(x => {
        BigDecimal(x) > 0
      })
      .attempt
      .map(_.leftMap(_ => BalanceTooLow).void)
}

