package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  // 1- inline configuration

  val configString =
    """
      |akka {
      | loglevel = "ERROR"
      |}""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])

  //actor ! "A message to remember"

  // 2 - config file
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  //defaultConfigActor ! "remember me"

  // 3 - separate config in the same file
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "remember me i am special"

  // 4 - separate config in another file

  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"seperate config log level: ${separateConfig.getString("akka.loglevel"
  )}")

  // 5- different file formats
  // JSON ,properties
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config: ${jsonConfig.getString("aJsonProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"props config: ${propsConfig.getString("my.simpleProperty")}")
  println(s"props config: ${propsConfig.getString("akka.loglevel")}")

}
