import Common._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props, RootActorPath}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey}
import akka.cluster.ddata.Replicator._
import akka.dispatch.forkjoin.ThreadLocalRandom
import com.typesafe.config.ConfigFactory


class ClusterChild(id:Int, hash : String, version: Int) extends Actor  with ActorLogging  {
  val cluster = Cluster(context.system)

  implicit val node = Cluster(context.system)
  val DataKey = ORSetKey[String]("key")
  val replicator = DistributedData(context.system).replicator
  replicator ! Subscribe(DataKey, self)


  override def preStart( ): Unit = {
    println(s"$self preStart")
    cluster.subscribe(self, classOf[ MemberUp ])
  }
  override def postStop( ): Unit = {
    cluster.unsubscribe(self)
    println(s"$self postStop")
   }

  def receive: Receive = {
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case MemberUp(member) => {
      if (member.hasRole("ClusterMother")) {
        context.actorSelection(RootActorPath(member.address) / "user" / "ClusterMother") !
          RegisterQuery(id, hash, version)
      }
    }
    case SendOrder => {
      println(s"$self Send Order ! $version")
      sender ! SendOrderReply
      var reply: String = ""
     // if (ThreadLocalRandom.current().nextBoolean()) {
        reply = s"Add $id"
        replicator ! Update(DataKey, ORSet.empty[String], WriteLocal)(_ + id.toString)
   /*   }
      else {
        reply = s"Less $id"

        replicator ! Update(DataKey, ORSet.empty[String], WriteLocal)(_ - id.toString)
      }*/
      sender ! SendOrderReply (reply)
      replicator ! Get(DataKey, ReadLocal)
    }
    case SendOrderSelfDestruct => {

      cluster.unsubscribe(self)
      context.system.terminate()
    }
    case GetVersion => {
      println(s"$self [${System.getProperty("os.name")} - ${System.getProperty("os.version")}]$version")
      sender ! GetVersionReply(s"[${System.getProperty("os.name")} - ${System.getProperty("os.version")}] $version")
    }
    case ex :Exception =>{
      println(s" Receive this ex : $ex")
    }
    case c @ Changed(DataKey) =>{
      val data = c.get(DataKey)
      log.info("Current elements: {}", data.elements)
    }
    case g @ GetSuccess(DataKey, req) => println(s"[$self] val : ${g.get(DataKey).elements}")
    case  UpdateSuccess(DataKey, req) => println(s"req : $req")
  }
}

object ClusterChild {

  def props(id: Int, hash : String, version: Int) = Props(new ClusterChild(id, hash, version))

  def main(args: Array[String]): Unit = {
    if (args.length != 4)
      throw new Error("Not enough parameter ./ClusterChild Port Id hash Version")
    initiate(args(0).toInt, args(1).toInt, args(2), args(3).toInt)
  }
  def initiate (port:Int, id : Int, hash: String, version: Int) = {

    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.load().getConfig("ClusterChild"))
    val system = ActorSystem("ClusterSystem", config)
    system.actorOf(props(id, hash, version))
  }
}
