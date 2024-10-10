package com.egg.smapi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.SystemMaterializer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import scala.util.{Failure, Success, Try}
import io.circe.Json
import io.circe.generic.auto._
import com.egg.smapi.SchemaDefinition.schema

object Server extends App {

  implicit val system: ActorSystem = ActorSystem("graphql-server")
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer = SystemMaterializer(system).materializer

  // Define the route
val route =
  path("graphql") {
    post {
      entity(as[String]) { requestJson =>
        val query = parseRequest(requestJson)

        query match {
          case Success(QueryMessage(query, operationName, variables)) =>
            complete(executeGraphQLQuery(query, operationName, variables))
          case Failure(error) =>
            complete(HttpResponse(StatusCodes.BadRequest, entity = s"Invalid request: ${error.getMessage}"))
        }
      }
    }
  }


  // Start the server
  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(route)

  // Handle successful binding and failures
  bindingFuture.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println(s"Server is running at http://${address.getHostString}:${address.getPort}/graphql")
    case Failure(exception) =>
      println(s"Failed to bind HTTP server: ${exception.getMessage}")
      system.terminate()
  }

  // Keep the server running until system termination
  Await.result(system.whenTerminated, Duration.Inf)

  // Method to parse the incoming request JSON
  def parseRequest(jsonString: String): Try[QueryMessage] = {
    io.circe.parser.parse(jsonString) match {
      case Right(json) =>
        Try {
          QueryMessage(
            query = json.hcursor.get[String]("query").getOrElse(""),
            operationName = json.hcursor.get[Option[String]]("operationName").toOption.flatten,
            variables = json.hcursor.get[Option[Json]]("variables").toOption.flatten.getOrElse(Json.obj())
          )
        }
      case Left(error) =>
        Failure(error)
    }
  }

  // Method to execute the GraphQL query
def executeGraphQLQuery(query: String, operationName: Option[String], variables: Json): Future[HttpResponse] = {
  QueryParser.parse(query) match {
    case Success(queryAst: Document) =>
      Executor.execute(
        schema = schema,
        queryAst = queryAst,
        userContext = new MyContext,
        variables = if (variables.isNull) Json.obj() else variables,
        operationName = operationName,
        middleware = KafkaMiddleware :: Nil // Register the middleware here
      ).map { result =>
        HttpResponse(
          status = StatusCodes.OK,
          entity = HttpEntity(ContentTypes.`application/json`, result.noSpaces)
        )
      }.recover {
        case error: QueryAnalysisError =>
          HttpResponse(StatusCodes.BadRequest, entity = error.resolveError.noSpaces)
        case error: ErrorWithResolver =>
          HttpResponse(StatusCodes.InternalServerError, entity = error.resolveError.noSpaces)
        case error =>
          HttpResponse(StatusCodes.InternalServerError, entity = error.getMessage)
      }

    case Failure(error) =>
      Future.successful(HttpResponse(StatusCodes.BadRequest, entity = s"Failed to parse GraphQL query: ${error.getMessage}"))
  }
}


}

// Case class to represent the GraphQL request message
case class QueryMessage(query: String, operationName: Option[String], variables: Json)

// Define your context class if needed
class MyContext {
  // Add any contextual data or services needed in your resolvers
}
