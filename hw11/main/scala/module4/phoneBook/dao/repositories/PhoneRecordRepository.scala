package module4.phoneBook.dao.repositories

import io.getquill.context.ZioJdbc._
import module4.phoneBook.dao.entities._
import module4.phoneBook.db
import zio.{Has, ULayer, ZLayer}
import io.getquill.{EntityQuery, Ord, Quoted}

object PhoneRecordRepository {
  val ctx = db.Ctx
  import ctx._

  type PhoneRecordRepository = Has[Service]

  trait Service{
      def find(phone: String): QIO[Option[PhoneRecord]]
      def list(): QIO[List[PhoneRecord]]
      def insert(phoneRecord: PhoneRecord): QIO[Unit]
      def update(phoneRecord: PhoneRecord): QIO[Unit]
      def delete(id: String): QIO[Unit]
  }

  class Impl extends Service{

    val phoneRecordSchema = quote{
      query[PhoneRecord]
    }
    val addressSchema = quote{
      query[Address]
    }


    override def find(phone: String): QIO[Option[PhoneRecord]] = {

      val q: Quoted[EntityQuery[PhoneRecord]] = quote{
        phoneRecordSchema.filter(_.phone == lift(phone))
      }
      // SELECT x2."id", x2."phone", x2."fio", x2."addressId" FROM "PhoneRecord" x2 WHERE x2."phone" = ?
      ctx.run(phoneRecordSchema.filter(_.phone == lift(phone))).map(_.headOption)
    }



    // SELECT x."id", x."phone", x."fio", x."addressId" FROM "PhoneRecord" 
    override def list(): QIO[List[PhoneRecord]] = ctx.run(phoneRecordSchema)

    // INSERT INTO "PhoneRecord" ("id","phone","fio","addressId") VALUES (?, ?, ?, ?)
    override def insert(phoneRecord: PhoneRecord): QIO[Unit] = ctx.run(
      phoneRecordSchema.insert(lift(phoneRecord))
    ).unit

    // INSERT INTO "PhoneRecord" ("id","phone","fio","addressId") VALUES (?, ?, ?, ?)
    def insert(phoneRecords: List[PhoneRecord]): QIO[Unit] = ctx.run(
      liftQuery(phoneRecords).foreach(p => phoneRecordSchema.insert(p))
    ).unit

    // UPDATE "PhoneRecord" SET "id" = ?, "phone" = ?, "fio" = ?, "addressId" = ? WHERE "id" = ?
    override def update(phoneRecord: PhoneRecord): QIO[Unit] = ctx.run(
      phoneRecordSchema.filter(_.id == lift(phoneRecord.id)).update(lift(phoneRecord))
    ).unit

    // DELETE FROM "PhoneRecord" WHERE "id" = ?
    override def delete(id: String): QIO[Unit] = ctx.run(
      phoneRecordSchema.filter(_.id == lift(id)).delete
    ).unit

    // implicit join
    // 
    val q1 = ctx.run(
      for{
        phr <- phoneRecordSchema
        address <- addressSchema if(address.id == phr.addressId)
      } yield phr
    )

    // applicative
    ctx.run(
      phoneRecordSchema.join(addressSchema).on(_.addressId == _.id)
    )

    // flat join
    ctx.run(
      for{
        phoneRecord <- phoneRecordSchema
        address <- addressSchema.join(_.id == phoneRecord.addressId)
      } yield phoneRecord
    )
  }

 
  val live: ULayer[PhoneRecordRepository] = ZLayer.succeed(new Impl) //???
}
