package Common


class ChildAction

case class SendOrder( ) extends ChildAction
case class SendOrderSelfDestruct( ) extends ChildAction
case class GetVersion() extends ChildAction
case class AskAllChildrenVersion() extends ChildAction
