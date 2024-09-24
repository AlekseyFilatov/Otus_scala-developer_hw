import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Props, _}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka_typed.TypedCalculatorWriteSide.{Add, Command, Divide, Multiply}
import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Props, _}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.GraphDSL.Implicits.{SourceShapeArrow, port2flow}
import akka_typed.CalculatorRepository.{getLatestsOffsetAndResultSlick, initDataBase, updatedResultAndOffsetSlick}
import akka_typed.TypedCalculatorWriteSide.{Add, Added, Command, Divide, Divided, Multiplied, Multiply}
import slick.backend.DatabaseConfig
import slick.basic.StaticDatabaseConfig
import slick.jdbc.PostgresProfile

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


case class Action(value: Int, name: String)


object akka_typed
{
  trait CborSerializable

  val persId = PersistenceId.ofUniqueId("001")

  object TypedCalculatorWriteSide {
    sealed trait Command
    case class Add(amount: Int)      extends Command
    case class Multiply(amount: Int) extends Command
    case class Divide(amount: Int)   extends Command

    sealed trait Event
    case class Added(id: Int, amount: Int)      extends Event
    case class Multiplied(id: Int, amount: Int) extends Event
    case class Divided(id: Int, amount: Int)    extends Event

    final case class State(value: Int) extends CborSerializable {
      def add(amount: Int): State      = copy(value = value + amount)
      def multiply(amount: Int): State = copy(value = value * amount)
      def divide(amount: Int): State   = copy(value = value / amount)
    }

    object State {
      val empty = State(0)
    }

    def apply(): Behavior[Command] =
      Behaviors.setup { ctx =>
        EventSourcedBehavior[Command, Event, State](
          persistenceId = persId,
          State.empty,
          (state, command) => handleCommand("001", state, command, ctx),
          (state, event) => handleEvent(state, event, ctx)
        )
      }

    def handleCommand(
        persistenceId: String,
        state: State,
        command: Command,
        ctx: ActorContext[Command]
    ): Effect[Event, State] =
      command match {
        case Add(amount) =>
          ctx.log.info(s"Receive adding for number: $amount and state is ${state.value}")
          val added = Added(persistenceId.toInt, amount)
          Effect
            .persist(added)
            .thenRun { x =>
              ctx.log.info(s"The state result is ${x.value}")
            }
        case Multiply(amount) =>
          ctx.log.info(s"Receive multiplying for number: $amount and state is ${state.value}")
          Effect
            .persist(Multiplied(persistenceId.toInt, amount))
            .thenRun { newState =>
              ctx.log.info(s"The state result is ${newState.value}")
            }
        case Divide(amount) =>
          ctx.log.info(s"Receive dividing for number: $amount and state is ${state.value}")
          Effect
            .persist(Divided(persistenceId.toInt, amount))
            .thenRun { x =>
              ctx.log.info(s"The state result is ${x.value}")
            }
      }

    def handleEvent(state: State, event: Event, ctx: ActorContext[Command]): State =
      event match {
        case Added(_, amount) =>
          ctx.log.info(s"Handing event amount is $amount and state is ${state.value}")
          state.add(amount)
        case Multiplied(_, amount) =>
          ctx.log.info(s"Handing event amount is $amount and state is ${state.value}")
          state.multiply(amount)
        case Divided(_, amount) =>
          ctx.log.info(s"Handing event amount is $amount and state is ${state.value}")
          state.divide(amount)
      }
  }

  case class TypedCalculatorReadSide(system: ActorSystem[NotUsed]) {
    initDataBase

    implicit val materializer            = system.classicSystem
    var (offset, latestCalculatedResult) = getLatestsOffsetAndResultSlick
    val startOffset: Int                 = if (offset == 1) 1 else offset + 1


//    val readJournal: LeveldbReadJournal =
    val readJournal: CassandraReadJournal =
      PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)


    /**
     * В read side приложения с архитектурой CQRS (объект TypedCalculatorReadSide в TypedCalculatorReadAndWriteSide.scala) необходимо разделить бизнес логику и запись в целевой получатель, т.е.
     * 1) Persistence Query должно находиться в Source
     * 2) Обновление состояния необходимо переместить в отдельный от записи в БД флоу
     * 3) ! Задание со звездочкой: вместо CalculatorRepository создать Sink c любой БД (например Postgres из docker-compose файла).
     * Для последнего задания пригодится документация - https://doc.akka.io/docs/alpakka/current/slick.html#using-a-slick-flow-or-sink
     * Результат выполненного д.з. необходимо оформить либо на github gist либо PR к текущему репозиторию.
     *
     * */


    val source: Source[EventEnvelope, NotUsed] = readJournal
      .eventsByPersistenceId("001", startOffset, Long.MaxValue)

    lazy val graph = akka.stream.scaladsl.RunnableGraph.fromGraph(GraphDSL.create()
    { implicit builder: GraphDSL.Builder[NotUsed] =>
      //1.

      val input = builder.add(source)
      val broadcast = builder.add(Broadcast[akka_typed.CalculatorRepository.Result](2))
      val stateUpdater = builder.add(Flow[EventEnvelope].map(e => updateState(e.event, e.sequenceNr)))
      val localSaveOutput = builder.add(Sink.foreach[akka_typed.CalculatorRepository.Result] {
        r =>
          latestCalculatedResult = r.state
          println(s"something to print -- latestCalculatedResult calculate: ${r.state}")
      })

      val dbSaveOutput = builder.add(
        Sink.foreach[akka_typed.CalculatorRepository.Result](r => updatedResultAndOffsetSlick(r.state, r.offset))
      )

      // надо разделить builder на 2  c помощью Broadcats
      //см https://blog.rockthejvm.com/akka-streams-graphs/

      //надо будет сохранить flow(разделенный на 2) в localSaveOutput и dbSaveOutput
      //в конце закрыть граф и запустить его RunnableGraph.fromGraph(graph).run()
      input ~> stateUpdater ~> broadcast
      broadcast.out(0) ~> localSaveOutput
      broadcast.out(1) ~> dbSaveOutput

      akka.stream.ClosedShape
    })


    /*source
      .map{x =>
        println(x.toString())
        x
      }
      .runForeach { event =>
      event.event match {
        case Added(_, amount) =>
//          println(s"!Before Log from Added: $latestCalculatedResult")
          latestCalculatedResult += amount
          updatedResultAndOffsetSlick(latestCalculatedResult, event.sequenceNr)
          println(s"! Log from Added: $latestCalculatedResult")
        case Multiplied(_, amount) =>
//          println(s"!Before Log from Multiplied: $latestCalculatedResult")
          latestCalculatedResult *= amount
          updatedResultAndOffsetSlick(latestCalculatedResult, event.sequenceNr)
          println(s"! Log from Multiplied: $latestCalculatedResult")
        case Divided(_, amount) =>
//          println(s"! Log from Divided before: $latestCalculatedResult")
          latestCalculatedResult /= amount
          updatedResultAndOffsetSlick(latestCalculatedResult, event.sequenceNr)
          println(s"! Log from Divided: $latestCalculatedResult")
      }
    }*/

    graph.run()
    def updateState(event: Any, seqNum: Long): akka_typed.CalculatorRepository.Result ={
      val newState = event match {
        case Added(_, amount)=>
          latestCalculatedResult += amount
          updatedResultAndOffsetSlick(latestCalculatedResult, amount)
          println(s"Log from Added: $latestCalculatedResult")
          latestCalculatedResult
        case Multiplied(_, amount)=>
          latestCalculatedResult *= amount
          updatedResultAndOffsetSlick(latestCalculatedResult, amount)
          println(s"Log from Multiplied: $latestCalculatedResult")
          latestCalculatedResult
        case Divided(_, amount)=>
          latestCalculatedResult /= amount
          updatedResultAndOffsetSlick(latestCalculatedResult, amount)
          println(s"Log from Divided: $latestCalculatedResult")
          latestCalculatedResult
      }
      akka_typed.CalculatorRepository.Result(newState, seqNum)
    }
  }

  object CalculatorRepository {
    import scalikejdbc._
    case class Result(state: Double, offset:Long)
    def initDataBase: Unit = {
      Class.forName("org.postgresql.Driver")
      val poolSettings = ConnectionPoolSettings(initialSize = 10, maxSize = 100)

      ConnectionPool.singleton("jdbc:postgresql://localhost:5432/demo", "docker", "docker", poolSettings)
    }

    // homework
    // case class Result(state: Double, offset:Long)
    /*    def getLatestsOffsetAndResult: Result ={
          val query = sql"select * from public.result where id = 1;"
            .as[Double]
            .headOption
          //надо создать future для db.run
          //с помошью await получите результат или прокиньте ошибку если результат нет

        }*/
    @StaticDatabaseConfig("file:src/main/resources/application.conf#sql")
    def getLatestsOffsetAndResultSlick: (Int, Double) ={
      /*для тестирования установлено два профайла*/
      val dc: slick.basic.DatabaseConfig[PostgresProfile] = slick.basic.DatabaseConfig.forAnnotation[slick.jdbc.PostgresProfile]

      import dc.driver.api._
      val db = dc.db
      import akka_typed.ModelImp._

      /*scala.concurrent.Await.result(db.run(DBIO.seq(
        slick.jdbc.meta.MTable.getTables map (cat => {println(cat.toString())}))), timeout.duration)
      println("create table!!!!!!")*/
      val futureQuery = db.run(resultTable.map(row => (row.write_side_offset,row.calculated_value))
          .result).map(_.head)
        .map(row => (row._1, row._2.toDouble))

      futureQuery.onComplete {
        case scala.util.Success(value) => println(s"Select Success! value: ${value}")
        case scala.util.Failure(e) => new Throwable(s"Error select! message ${e.getMessage}")
      }
      /* val query = sql"""select write_side_offset,calculated_value from "public.result" where id = 1;"""
        .as[(Int, Double)].headOption*/
      scala.concurrent.Await.result(futureQuery, timeout.duration)
    }

    @StaticDatabaseConfig("file:src/main/resources/application.conf:#sql")
    def updatedResultAndOffsetSlick(calculated: Double, offset: Long): Unit ={
      import akka_typed.ModelImp._
      val dc = DatabaseConfig.forConfig[slick.jdbc.PostgresProfile]("slick")
      import dc.driver.api._
      val db = dc.db
      val query = resultTable.map(r => (r.id,r.calculated_value,r.write_side_offset)).update(1,calculated.toFloat,offset.toInt)
      //val query = sqlu"""update "public.result" set "calculated_value" = #${calculated}, "write_side_offset" = #${offset} where id = 1;"""
      //val futureRun = dc.db.run(query.transactionally.withTransactionIsolation(ti = slick.jdbc.TransactionIsolation.ReadCommitted))
      val futureRun = dc.db.run(query)
      futureRun.onComplete {
        case scala.util.Success(value) => {
          println(s"Update Success! value: ${value}")
          scala.util.Success(value)
        }
        case scala.util.Failure(e) => {
          println(s"Error update! message: ${e.getMessage}")
          Failure(e)
        }
      }
      scala.concurrent.Await.result(futureRun, timeout.duration)

    }

    /*def getLatestOffsetAndResult: (Int, Double) = {
      val entities =
        DB readOnly { session =>
          session.list("select * from public.result where id = 1;") {
            row => (row.int("write_side_offset"), row.double("calculated_value")) }
        }
      entities.head
    }*/

    /*def updateResultAndOfsset(calculated: Double, offset: Long): Unit = {
      using(DB(ConnectionPool.borrow())) { db =>
        db.autoClose(true)
        db.localTx {
          _.update("update public.result set calculated_value = ?, write_side_offset = ? where id = ?", calculated, offset, 1)
        }
      }
    }*/
  }


  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val writeActorRef = ctx.spawn(TypedCalculatorWriteSide(), "Calculato", Props.empty)

      writeActorRef ! Add(10)
      writeActorRef ! Multiply(2)
      writeActorRef ! Divide(5)

      Behaviors.same
    }

  def execute(comm: Command): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val writeActorRef = ctx.spawn(TypedCalculatorWriteSide(), "Calculato", Props.empty)

      writeActorRef ! comm

      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    val value = akka_typed()
    implicit val system: ActorSystem[NotUsed] = ActorSystem(value, "akka_typed")

    TypedCalculatorReadSide(system)

    implicit val executionContext = system.executionContext
  }

  object ModelImp{
    implicit val getCalculatorRepositoryResult: slick.jdbc.GetResult[(Int, Double)] = slick.jdbc.GetResult(r => (r.nextInt(), r.nextDouble()))
    implicit val timeout: akka.util.Timeout = new akka.util.Timeout(10.seconds)
    //val dc: slick.basic.DatabaseConfig[PostgresProfile] = slick.basic.DatabaseConfig.forAnnotation[slick.jdbc.PostgresProfile]
    val dc = DatabaseConfig.forConfig[slick.jdbc.PostgresProfile]("slick")
    import dc.driver.api._
    val db = dc.db
    case class ResultType(id:Int,calculated_value:Float,write_side_offset:Int)
    class ResultTable(tag: Tag) extends Table[ResultType](tag, Some("public"), "result") {
      def id: Rep[Int] = column[Int]("id")
      def calculated_value: Rep[Float] = column[Float]("calculated_value")
      def write_side_offset: Rep[Int] = column[Int]("write_side_offset")
      override def *  = (id,calculated_value,write_side_offset).mapTo[ResultType]
    }
    lazy val resultTable = TableQuery[ResultTable]
    import slick.jdbc.SetParameter

    /*implicit val SetCalculated = SetParameter[Double](
      (calculated, pp) => pp.setDouble(calculated))

    implicit val SetOffset = SetParameter[Long](
      (offset, pp) => pp.setLong(offset))*/
  }
}