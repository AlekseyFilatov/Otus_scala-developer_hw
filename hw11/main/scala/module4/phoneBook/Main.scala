package module4.phoneBook

//import module3.emailService.EmailService
//import module3.userDAO.UserDAO
//import module3.userService.{UserID, UserService}
import zio._
import zio.console.Console


object Main extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    App.server.exitCode
}
