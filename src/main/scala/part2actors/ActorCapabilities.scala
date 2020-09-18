package part2actors

import akka.actor.FSM.Failure
import akka.actor.Status.Success
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.MyBankAccount.Withdraw
import part2actors.ActorCapabilities.Person.LiveTheLife

object ActorCapabilities extends App{

  class SimpleActor extends Actor {

    override def receive: Receive = {
      case "Hi" => context.sender() ! "hello, there!"
      case message: String => println(s"[${self}] I have received $message")
      case number: Int => println(s"[Simple Actor] I have received $number")
      case SpecialMessage(contents) => println(s"[Simple Actor] I have received $contents")
      case SendMessageToYourself(content) =>
        self ! content
      case SayHiTo(ref) => ref ! "Hi" // alice is being passed as sender
      case WirelessPhoneMessage(content, ref) => ref forward  (content + "s") // I keep the originial sender of the WPM
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  //simpleActor ! "hello, Actor"

  // 1- message can be of any type
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE

  // in practive use case cs and case objects
  //simpleActor ! 42

  case class SpecialMessage(contents:String)
  //simpleActor ! SpecialMessage("some special message")

  // 2 - actors have information about their ocontext and about themselves
  // context.self == this
  case class SendMessageToYourself(content: String)
  //simpleActor ! SendMessageToYourself("I am an Actor and I am proud of it")



  // 3 - actors can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)

  //alice ! SayHiTo(bob)

  // 4 - dead letters
  //alice ! "Hi" // reply to me

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a message with the ORIGINIAL sender

  case class WirelessPhoneMessage(context: String, ref: ActorRef)
  //alice ! WirelessPhoneMessage("Hi", bob) // no sender


  /**
   * Exercises
   *
   * 1. a Counter actor
   *  - Increment
   *  - Decrement
   *  - Print
   *
   *  2. a Bank account as an actor
   *
   *    receives
   *    - Deposit an amount
   *    - Withdraw an amount
   *    - Statement
   *    replies with
   *    - Success
   *    - Failure
   *
   *    interact with some other kind of actor
   *
   *
   */

  // DOMAIN of the counter
  object Counter{
    case object Increment
    case object Decrement
    case object Print
  }
  class Counter extends Actor {
    import Counter._
    var counter: Int = 0

    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[Counter] The value of counter is ${counter}")
    }
  }

  import Counter._
  val myCounter = system.actorOf(Props[Counter], "myCounter")
/*
  myCounter ! Increment
  myCounter ! Increment
  myCounter ! Decrement
  myCounter ! Print
*/


  object BankAccount {
    def props(initialBalance: Int) = Props(new BankAccount(100))


  }

  class BankAccount(initialBalance: Int) extends Actor{
    var balance: Int = initialBalance
    override def receive: Receive = {
      case Operation(amount) =>
        balance += amount
      case 0 => println(s"[BankAccount] Your current balance is $balance")
      case _ =>
    }
  }

  class BankAccountCaller extends Actor{
    override def receive: Receive = {
      case OperationCall(amount, ref) => ref ! Operation(amount)
      case StatementRequest(amount, ref) => ref ! amount
      case Success => println("Operation is successful")
      case Failure => println("Operation is failed")
    }
  }

  case class Operation(amount: Int)
  case class OperationCall(amount: Int, actorRef: ActorRef)
  case class StatementRequest(amount: Int, actorRef: ActorRef)

  val myBankAccount = system.actorOf(BankAccount.props(100), "myBankAccount")
  val myBankAccountCaller = system.actorOf(Props[BankAccountCaller], "myBankAccountCaller")

  /*myBankAccountCaller ! OperationCall(50, myBankAccount)
  myBankAccountCaller ! OperationCall(-20, myBankAccount)
  myBankAccountCaller ! StatementRequest(0, myBankAccount)
*/


  object MyBankAccount {
    case class Deposit(amount:Int)
    case class Withdraw(amount:Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }

  class MyBankAccount extends Actor {
    import MyBankAccount._
    var funds = 0
    override def receive: Receive = {
      case Deposit(amount) =>
        if(amount < 0) sender() ! TransactionFailure("invalid deposit amount")
        else{
          funds += amount
          sender() ! TransactionSuccess(s"successfully deposited $amount")
        }
      case Withdraw(amount) =>
        if(amount<0) sender() ! TransactionFailure("invalid withdraw amount")
        else if (amount > funds) sender() ! TransactionFailure("insufficient funds")
        else{
          funds -= amount
          sender() ! TransactionSuccess(s"Succesfully withdrew $amount")
        }

      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  object Person{
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor{
    import Person._
    import MyBankAccount._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }


  val newBankAccount = system.actorOf(Props[MyBankAccount], "newBankAccount")
  val person = system.actorOf(Props[Person], "billionaire")

  person ! LiveTheLife(newBankAccount)



}
