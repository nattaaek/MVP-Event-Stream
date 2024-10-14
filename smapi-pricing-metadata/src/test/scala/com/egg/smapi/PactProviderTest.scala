package com.egg.smapi

import org.scalatest.funsuite.AnyFunSuite
import pact4s.scalatest.MessagePactVerifier
import pact4s.provider._
import io.circe.syntax._
import io.circe.generic.auto._
import com.egg.smapi.KafkaMiddleware.KafkaMessage
import pact4s.provider.ResponseBuilder.MessageAndMetadataBuilder
import io.circe.Json

class PactProviderVerificationSpec extends AnyFunSuite with MessagePactVerifier {
  // Define the provider details and the Pact source
  override val provider: ProviderInfoBuilder = ProviderInfoBuilder(
    name = "SMAPIPricingMetadata",
    pactSource = PactSource.PactBrokerWithSelectors(
      brokerUrl = "http://localhost:9292"
    )
  )

  // Define the messages that need to be verified
  override def messages: String => ResponseBuilder = {
    case "a promotion message" =>
      val messageContent = KafkaMessage(
        actionType = "CREATE",
        parameters = Map(
          "discount" -> Json.fromInt(20),
          "validity" -> Json.fromString(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
        ),
        userId = Some("user123") // Optional field
      )

      // Return the message content and metadata for Pact verification
      MessageAndMetadataBuilder(
        message = messageContent.asJson.noSpaces,
        metadata = Map("contentType" -> "application/json")
      )
  }

  // Define the test that sends the message and runs the verification
  test("pact verification for a promotion message") {
    verifyPacts(
      providerBranch = Some(Branch("main")),
      publishVerificationResults = Some(
        PublishVerificationResults(
          providerVersion = "1.0.0"
        )
      )
    )
  }
}
