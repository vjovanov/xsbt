/* sbt -- Simple Build Tool
 * Copyright 2010 Mark Harrah
 */
package sbt

import Incomplete.{Error, Value => IValue}
final case class Incomplete(node: Option[AnyRef], tpe: IValue = Error, message: Option[String] = None, causes: Seq[Incomplete] = Nil, directCause: Option[Throwable] = None)
	extends Exception(message.orNull, directCause.orNull) {
		override def toString = "Incomplete(node=" + node + ", tpe=" + tpe + ", msg=" + message + ", causes=" + causes + ", directCause=" + directCause +")"
}

object Incomplete extends Enumeration {
	val Skipped, Error = Value
	
	def transform(i: Incomplete)(f: Incomplete => Incomplete): Incomplete =
	{
			import collection.JavaConversions._
		val visited: collection.mutable.Map[Incomplete,Incomplete] = new java.util.IdentityHashMap[Incomplete, Incomplete]
		def visit(inc: Incomplete): Incomplete =
			visited.getOrElseUpdate(inc, visitCauses(f(inc)) )
		def visitCauses(inc: Incomplete): Incomplete =
			inc.copy(causes = inc.causes.map(visit) )

		visit(i)
	}
	def visitAll(i: Incomplete)(f: Incomplete => Unit)
	{
		val visited = IDSet.create[Incomplete]
		def visit(inc: Incomplete): Unit =
			visited.process(inc)( () ) {
				f(inc)
				inc.causes.foreach(visit)
			}
		visit(i)
	}
	def linearize(i: Incomplete): Seq[Incomplete] =
	{
		var ordered = List[Incomplete]()
		visitAll(i) { ordered ::= _ }
		ordered
	}
	def allExceptions(is: Seq[Incomplete]): Iterable[Throwable] =
		allExceptions(new Incomplete(None, causes = is))
	def allExceptions(i: Incomplete): Iterable[Throwable] =
	{
		val exceptions = IDSet.create[Throwable]
		visitAll(i) { exceptions ++= _.directCause.toList }
		exceptions.all
	}
	def show(tpe: Value) = tpe match { case Skipped=> "skipped"; case Error => "error" }
}