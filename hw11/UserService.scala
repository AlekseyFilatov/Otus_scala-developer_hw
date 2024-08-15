package module4.homework.services

import zio.Has
import zio.Task
import module4.homework.dao.entity.{Role, RoleCode, User, UserId, UserToRole}
import module4.homework.dao.repository.UserRepository
import zio.ZIO
import zio.RIO
import zio.ZLayer
import zio.macros.accessible
import module4.homework.dao.repository.UserRepository.UserRepository
import module4.phoneBook.db
import zio.ZIO.debug
//import module4.phoneBook.db.zioDS

@accessible
object UserService {
    type UserService = Has[UserService.Service]

    trait Service {
        def listUsers(): RIO[db.DataSource, List[User]]

        def listUsersDTO(): RIO[db.DataSource, List[UserDTO]]

        def addUserWithRole(user: User, roleCode: RoleCode): RIO[db.DataSource, UserDTO]

        def listUsersWithRole(roleCode: RoleCode): RIO[db.DataSource, List[UserDTO]]
    }


    class Impl(userRepo: UserRepository.Service) extends Service{
        val dc: db.Ctx.type = db.Ctx
        import dc._

        def listUsers(): RIO[db.DataSource, List[User]] =
        userRepo.list()


        def listUsersDTO(): RIO[db.DataSource,List[UserDTO]] = {
           for {
                  usrDto <- userRepo.listUsersDTO().mapEffect(userDTO => userDTO.map(usr => UserDTO(usr._1,usr._2)))
                } yield usrDto
        }
        //???
        
        def addUserWithRole(user: User, roleCode: RoleCode): RIO[db.DataSource, UserDTO] = {
            val a = for {
                usr <- dc.transaction(
                    for {
                        usr <- userRepo.createUser(user)
                        _ <- userRepo.insertRoleToUser(roleCode = roleCode, usr.typedId)
                    } yield usr )
                //_ <- ZIO.effect(println("xxxxxxxxxxxxxxxxxxxxxxxx"))
                //_ <- ZIO.effect(println(s"${usr.id} ,${usr.firstName} ,${usr.lastName} ,${usr.age}"))
                role <- userRepo.userRoles(usr.typedId)
                //_ <- ZIO.effect(println(s"role: ${role.head.code} ${role.head.name}"))
            } yield UserDTO(user = usr, roles = role.toSet)
            a
        }
        //???

        def listUsersWithRole(roleCode: RoleCode): RIO[db.DataSource,List[UserDTO]] = for {
            listUsers <- userRepo.listUsersWithRole(roleCode = roleCode)
            role <- userRepo.findRoleByCode(roleCode = roleCode)
            userDto <- ZIO.succeed(listUsers.map(usr => UserDTO(usr, Set(role.get))))
        } yield userDto
        //???
        
        
    }

    val live: ZLayer[UserRepository.UserRepository, Nothing, UserService] =
        ZLayer.fromService[UserRepository.Service, module4.homework.services.UserService.Service](userR => new Impl(userR))

}


case class UserDTO(user: User, roles: Set[Role])
