package com.egg.smapi

import sangria.schema._
import sangria.macros.derive._
import io.circe.generic.auto._
import sangria.marshalling.circe._
import io.circe.Encoder
import io.circe.generic.semiauto._
import scala.concurrent.Future

object SchemaDefinition {

  // Define your case classes
  case class PromotionInput(
    promotionId: String,
    actionTypeId: String,
    parameters: Parameters
  )

  case class Parameters(
    discount: Int,
    validity: String
  )

  case class PromotionResponse(
    promotionId: String,
    actionTypeId: String
  )

  object PromotionInput {
    implicit val encoder: Encoder[PromotionInput] = deriveEncoder[PromotionInput]
  }
  object Parameters {
    implicit val encoder: Encoder[Parameters] = deriveEncoder[Parameters]
  }

  // Define the InputObjectType for Parameters
  implicit val ParametersInputType: InputObjectType[Parameters] = deriveInputObjectType[Parameters]()

  // Define the InputObjectType for PromotionInput
  implicit val PromotionInputType: InputObjectType[PromotionInput] = deriveInputObjectType[PromotionInput]()

  // Define the ObjectType for PromotionResponse
  val PromotionResponseType: ObjectType[Unit, PromotionResponse] = deriveObjectType[Unit, PromotionResponse]()

  // Define the MutationType
val MutationType: ObjectType[Unit, Unit] = ObjectType(
  "Mutation",
  fields[Unit, Unit](
    Field(
      name = "Promotion",
      fieldType = PromotionResponseType,
      arguments = Argument("input", PromotionInputType) :: Nil,
      resolve = ctx => {
        val input = ctx.args.arg[PromotionInput]("input")
        // Your logic here
        Future.successful(PromotionResponse(promotionId = input.promotionId, actionTypeId = input.actionTypeId))
      }
    )
  )
)


  // Define an empty QueryType
  val QueryType: ObjectType[Unit, Unit] = ObjectType(
    "Query",
    fields[Unit, Unit](
      Field(
        name = "dummy",
        fieldType = StringType,
        description = Some("A dummy field."),
        resolve = _ => "This is a dummy field."
      )
    )
  )

  // Define the schema with both query and mutation types
  val schema: Schema[Any, Unit] = Schema(
    query = QueryType,
    mutation = Some(MutationType)
  )
}
