package Common

class ChildReply

case class SendOrderReply(reply: String) extends ChildReply
case class SendOrderSelfDestructReply() extends ChildReply
case class GetVersionReply(version: String) extends ChildReply