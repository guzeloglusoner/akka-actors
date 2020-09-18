package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActorExercise.WordCounterMaster.{WordCountReply, WordCountTask}

import scala.collection.immutable

object ChildActorExercise extends App {

  // Distributed Word Counting

  object WordCounterMaster {

    case class Initialize(nChildren: Int)

    case class WordCountTask(id: Int, text: String)

    case class WordCountReply(id: Int, count: Int)

  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = initialization

    def initialization: Receive = {
      case Initialize(nChildren) =>
        println("[master] initializing..")
        val workerRefs: immutable.Seq[ActorRef] = (1 to nChildren).map(e => context.actorOf(Props[WordCounterWorker], s"worker${e.toString}"))
        context.become(readyForTask(workerRefs, 0, 0, Map()))
    }

    def readyForTask(assemblyLine: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[Master] I have received $text I will send it to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = assemblyLine(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % assemblyLine.length
        val newTaskId = currentTaskId + 1
        val newRequestMap  = requestMap + (currentTaskId -> originalSender)
        context.become(readyForTask(assemblyLine, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I have received a reply for task id $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(readyForTask(assemblyLine, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I have received task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
      case _ => println("I can only count")
    }
  }

  import WordCounterMaster._

  val system = ActorSystem("RoundRobinWorkCountExercise")

  class TestActor extends Actor {
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val texts = List("I love akka", "Scala is super dope", "yes", "me too")
        texts.foreach(master ! _)
      case count: Int =>
        println(s"[test actor] I received a reply: $count")
    }
  }

  val testActor = system.actorOf(Props[TestActor], "testActor")

  testActor ! "go"



  /*
      create WordCounterMaster
        send Initialize(10) to wordCounterMaster
        send "Akka is awesome" to wordCounterMaster
          wcm will send a  WordCountTask("..") to one of its children
            child replies with a WordCountReply(3) to the master
          master replies with 3 to the sender.



         requester -> wcm -> wcw
                 r  <- wcm  <-
   */

  // round robin logic
  // 1,2,3,4,5 children and 7 tasks
  // 1,2,3,4,5,1,2

}
