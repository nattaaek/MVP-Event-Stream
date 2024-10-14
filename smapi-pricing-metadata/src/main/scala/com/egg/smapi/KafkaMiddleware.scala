package com.egg.smapi

import sangria.execution._
import sangria.schema._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.circe.syntax._
import io.circe.Json
import sangria.ast

object KafkaMiddleware extends Middleware[MyContext] with MiddlewareAfterField[MyContext] {
  type QueryVal = Unit
  type FieldVal = Unit

  case class KafkaMessage(
    actionType: String,
    parameters: Map[String, Json],
    userId: Option[String]
  )

  // Convert Map[String, Any] to Map[String, Json]
  private def convertToJson(parameters: Map[String, Any]): Map[String, Json] = {
    parameters.map {
      case (key, value: String) => key -> Json.fromString(value)
      case (key, value: Int) => key -> Json.fromInt(value)
      case (key, value: Long) => key -> Json.fromLong(value)
      case (key, value: Double) => key -> Json.fromDoubleOrNull(value)
      case (key, value: Float) => key -> Json.fromFloatOrNull(value)
      case (key, value: Boolean) => key -> Json.fromBoolean(value)
      case (key, value: Map[_, _]) =>
        val nestedMap = value.asInstanceOf[Map[String, Any]]
        key -> Json.obj(convertToJson(nestedMap).toSeq: _*)
      case (key, value: Seq[_]) =>
        key -> Json.arr(value.map(v => convertToJson(Map("value" -> v)).values.head): _*)
      case (key, value: Option[_]) =>
        key -> value.map(v => convertToJson(Map("value" -> v)).values.head).getOrElse(Json.Null)
      case (key, value) =>
        key -> Json.fromString(value.toString)
    }
  }

  // Check if the operation is a mutation
  private def isMutation(context: MiddlewareQueryContext[Any, _, _]): Boolean = {
    context.queryAst.definitions.collect {
      case op: ast.OperationDefinition if op.operationType == ast.OperationType.Mutation => op
    }.nonEmpty
  }

  // This is called before the query starts.
  override def beforeQuery(context: MiddlewareQueryContext[MyContext, _, _]): QueryVal = ()

  // This is called before a field is resolved.
  override def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[MyContext, _, _], ctx: Context[MyContext, _]): BeforeFieldResult[MyContext, FieldVal] = continue

  // This is called after the query completes.
  override def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[MyContext, _, _]): Unit = ()

  // This is called after a field resolves.
  override def afterField(
    queryVal: QueryVal,
    fieldVal: FieldVal,
    value: Any,
    mctx: MiddlewareQueryContext[MyContext, _, _],
    ctx: Context[MyContext, _]
  ): Option[Any] = {
    // Only proceed if this is a mutation and this field is the root mutation field.
    if (isMutation(mctx) && ctx.parentType.name == "Mutation") {
      val actionType = ctx.field.name
      val rawParameters = ctx.args.raw
      val jsonParameters = convertToJson(rawParameters)
      val userId = ctx.ctx.userId

      val message = KafkaMessage(
        actionType = actionType,
        parameters = jsonParameters,
        userId = userId
      )

      // Send the message and return the original value.
      KafkaProducerApp.sendMessage("activation", message, actionType)
      Some(value)
    } else {
      None
    }
  }
}
