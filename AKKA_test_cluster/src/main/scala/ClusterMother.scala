
import ApprovalActor._
import Common._
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.ClusterEvent._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ClusterMother extends Actor with ActorLogging {
  private var childs = Set.empty[ ActorRef ]
  private var childVersion: List[(ActorRef, Int)] = List()

  def receive: Receive = {
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

      case RegisterQuery(id: Int, hash: String, version: Int) => {
        println(s"tentative to register : $sender")
        val origSender = sender // https://stackoverflow.com/a/16898402/1150229
      implicit val timeout = Timeout(10 seconds)
      val approvalActor = context.actorOf(ApprovalActor.props)
        import context.dispatcher

      (approvalActor ? CheckIdAndHash(id, hash)) onComplete {
        case Success(v) => v match {
          case true => {
            println(s"[APPROVED]tentative to register : $origSender")
            childs += origSender
            childVersion  = (origSender, version ) :: childVersion
            context.watch(origSender)
            RetrieveAndKillOutdatedChildren()
          }
          case false => {
            println(s"[REFUSED]tentative to register : $origSender")
            origSender ! SendOrderSelfDestruct
            throw new Error("Invalid Child !")
          }
        }
        case Failure(f) => throw new Error(s"Error : $f")
      }
    }
    case  SendOrderReply (reply: String)=> {
      println(s"$sender SendOrderReply : {$reply")
    }
    case  SendOrderSelfDestructReply() => {
      println(s"$sender SendOrderSelfDestructReply")
    }
    case GetVersionReply(version: String) => {
      println(s"$sender version : $version ")
    }
    case AskAllChildrenVersion => {
      if (childs.size > 0 ) {
        childs foreach {x =>  x !  GetVersion
        x ! SendOrder}
      }
    }
  }

  def ChildToDelete(toDelete :(ActorRef, Int)) = {
    childs -= toDelete._1
    toDelete._1 !SendOrderSelfDestruct
  }

  def KillOutDatedChilds(maxVersion :  Int) = {

    for {
      outdated <- childVersion filter (_._2 < maxVersion)
    } yield ChildToDelete(outdated)

    childVersion = childVersion filter (_._2 >= maxVersion)
  }

  def RetrieveAndKillOutdatedChildren(): Unit = {
    println("RetrieveAndKillOutdatedChildren")
    KillOutDatedChilds((childVersion maxBy (_._2))._2)
  }
}

object ClusterMother {
  private var _frontend: ActorRef = _

  case object GetMemberNodes

  def initiate( ) = {
    val system = ActorSystem("ClusterSystem", ConfigFactory.load().getConfig("ClusterMother"))
    _frontend = system.actorOf(Props[ ClusterMother ], name = "ClusterMother")

    import system.dispatcher

    system.scheduler.schedule(
      0 milliseconds,
      5 seconds,
      _frontend,
      AskAllChildrenVersion)
  }


}
