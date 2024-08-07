package module4.homework.dao.repository

import zio.{Has, ULayer, ZIO, ZLayer}
import io.getquill.context.ZioJdbc._
import module4.homework.dao.entity.User
import zio.macros.accessible
import module4.homework.dao.entity.{Role, UserToRole}
import module4.homework.dao.entity.UserId
import module4.homework.dao.entity.RoleCode
import module4.phoneBook.db
import io.getquill.{EntityQuery, Ord, Quoted, Update}
import zio.ZIO.{debug, unit}

import java.sql.SQLException
import javax.sql.DataSource


object UserRepository{

    val dc: db.Ctx.type = db.Ctx
    import dc._

    type UserRepository = Has[UserRepository.Service]

    trait Service{
        def findUser(userId: UserId): QIO[Option[User]]
        def createUser(user: User): QIO[User]
        def createUsers(users: List[User]): QIO[List[User]]
        def updateUser(user: User): QIO[Unit]
        def deleteUser(user: User): QIO[Unit]
        def findByLastName(lastName: String): QIO[List[User]]
        def list(): QIO[List[User]]
        def userRoles(userId: UserId): QIO[List[Role]]
        def insertRoleToUser(roleCode: RoleCode, userId: UserId): QIO[Unit]
        def listUsersWithRole(roleCode: RoleCode): QIO[List[User]]
        def findRoleByCode(roleCode: RoleCode): QIO[Option[Role]]
        def listUsersDTO(): QIO[List[(User, Set[Role])]]
    }

    class ServiceImpl extends Service{


        val userSchema = quote{
            query[User]
        }
        val roleSchema = quote{
            query[Role]
        }
        val userToRoleSchema = quote{
            query[UserToRole]
        }

        override def listUsersDTO(): QIO[List[(User,Set[Role])]] = {
         for{
             listRole <- dc.run(roleSchema.distinct)
             listUsr <- dc.run(userToRoleSchema.join(roleSchema)
               .on((utr, r) => utr.roleId == r.code)
               .join(userSchema)
               .on((utrr, users) => utrr._1.userId == users.id)
               .map(rr => (User(rr._2.id, rr._2.firstName, rr._2.lastName, rr._2.age),
                 Role(rr._1._2.code, rr._1._2.name))).groupBy(_._1)
               .map(rr => (rr._1, infix"array_agg(${rr._2.map(role => role._2.code)})"
                 .pure.as[List[String]])))
           } yield listUsr.map(usr => (usr._1,usr._2.map(code => listRole.find(lr => lr.code == code).get).toSet))
        }

        override def findUser(userId: UserId): QIO[Option[User]] = {

            /*val q: Quoted[EntityQuery[User]] = quote{
                userSchema.filter(_.id == lift(userId.id))
            }*/
            // SELECT x2."id", x2."phone", x2."fio", x2."addressId" FROM "PhoneRecord" x2 WHERE x2."phone" = ?
            dc.run(userSchema.filter(_.id == lift(userId.id))).map(_.headOption)
        }//???

        override def createUser(user: User): QIO[User] = {
           /* val qs = dc.quote{ querySchema[User]("users")}
            val q = dc.run {
                dc.quote {
                    qs.insert(lift(user))
                }
            }*/
           dc.run(userSchema.insert(lift(user)).returning(usr => usr))
          //  .mapBoth(e => new Throwable(e.getMessage), _ => user)
          //   .orElseFail(new Throwable("fail create record"))
            //???
        }

        override def createUsers(users: List[User]): QIO[List[User]] = {
          dc.run(liftQuery(users).foreach(p => userSchema.insert(p)
             .returning(usr => usr)))
        }
        //???

        override def updateUser(user: User): QIO[Unit] =
            dc.run(userSchema.filter(usr => usr.id == lift(user.id)).update(lift(user))).unit
        //???

        override def deleteUser(user: User): QIO[Unit] =
            dc.run(userSchema.filter(usr => usr.id == lift(user.id)).delete).unit
        //???

        override def findByLastName(lastName: String): QIO[List[User]] = dc.run(userSchema
          .filter(_.lastName == (lift(lastName))))
          //.mapBoth(e => new Throwable(e.getMessage), _.headOption.toList)
          //.orElseFail(new Throwable("record not found"))
        //???

        override def list(): QIO[List[User]] = dc.run(userSchema.distinct)
          // ====== Task[List[User]]
          // .mapError(e => new Throwable(e.getMessage)).provide(live)
          // перевод sql exeption to zio //???

        override def userRoles(userId: UserId): QIO[List[Role]] = {
                dc.run(userToRoleSchema
                  .join(roleSchema).on((utr, r) => utr.roleId == r.code)
                  .groupBy(_._2)
                  .map(ur => Role(ur._1.code, ur._1.name)))
        }
        //???

        override def insertRoleToUser(roleCode: RoleCode, userId: UserId): QIO[Unit] = {
            def insertRoleR(roleCode: RoleCode, userId: UserId): QIO[Unit] = for {
                _ <- dc.run(roleSchema.insert(Role(lift(roleCode.code),name = lift(roleCode.code.capitalize))))
                res <- dc.run(userToRoleSchema.insert(lift(UserToRole(roleCode.code, userId.id))))
            } yield res

            def updateRoleR(roleCode: RoleCode, userId: UserId): QIO[Unit] = for {
                _ <- dc.run(roleSchema.update(Role(lift(roleCode.code), name = lift(roleCode.code.capitalize))))
                res <- dc.run(userToRoleSchema.insert(lift(UserToRole(roleCode.code, userId.id))))
            } yield res

            def transactionRole(roleCode: RoleCode, userId: UserId): QIO[Unit] = {
                for {
                    countEmpty <- dc.run(roleSchema.filter(_.code == lift(roleCode.code))).map(_.headOption)
                    result <- if (countEmpty.isEmpty) {
                        insertRoleR(roleCode, userId)
                    } else {
                        updateRoleR(roleCode, userId)
                    }
                } yield result}

                transactionRole(roleCode, userId)
        }
        //???

        override def listUsersWithRole(roleCode: RoleCode): QIO[List[User]] =
            dc.run(userToRoleSchema.filter(_.roleId == lift(roleCode.code)).join(userSchema)
                  .on((utr, usr) => utr.userId == usr.id)
                  .groupBy(_._2).map(usr => usr._1)
                  .map({case user => User(user.id, user.firstName, user.lastName, user.age)})
                  .distinct)
        //???

        override def findRoleByCode(roleCode: RoleCode): QIO[Option[Role]] = {
           dc.run(roleSchema.filter(_.code == lift(roleCode.code)).distinct).map(_.headOption)
        }


    }

    val live: ULayer[UserRepository] = ZLayer.succeed(new UserRepository.ServiceImpl)
}