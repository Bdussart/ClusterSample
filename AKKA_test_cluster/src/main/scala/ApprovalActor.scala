import akka.actor.{Actor, Props}

object ApprovalActor {
  val props = Props[ApprovalActor]

  class ApprovalNewChild
  case class CheckIdAndHash(id : Int, hash: String) extends ApprovalNewChild

  class CheckAction
  case class checkId(id: Int) extends CheckAction
  case class checkHash(hash:String) extends CheckAction
}
class ApprovalActor extends Actor {
  import ApprovalActor._

  def checkId(id: Int)  : Boolean = ! ((2 until id-1) exists (id % _ == 0)) //isPrime
  def checkHash(hash : String): Boolean = {
    true
  }

  def receive : Receive = {
    case CheckIdAndHash (id, hash) => {
      sender ! (checkId(id) && checkHash(hash))
    }
  }

}
