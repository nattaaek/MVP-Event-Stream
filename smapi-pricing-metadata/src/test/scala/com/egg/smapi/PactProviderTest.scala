package com.egg.smapi

import org.scalatest.funsuite.AnyFunSuite
import pact4s.scalatest.MessagePactVerifier
import pact4s.provider._
import java.io.File
import pact4s.provider.ResponseBuilder.MessageAndMetadataBuilder
import io.circe.syntax._
import io.circe.generic.auto._
import com.egg.smapi.KafkaMiddleware.KafkaMessage

class PactProviderVerificationSpec extends AnyFunSuite with MessagePactVerifier {
  // Define the provider details and the Pact source
  override val provider: ProviderInfoBuilder = ProviderInfoBuilder(
    name = "SMAPIPricingMetadata",
    pactSource = PactSource.FileSource(
      ("ActivationConsumer", new File("pacts/ActivationConsumer-SMAPIPricingMetadata.json"))
    )
  )

  // Define the messages that need to be verified
  override def messages: String => ResponseBuilder = {
    case "a promotion message" =>
      // Generate the message content using KafkaMiddleware.KafkaMessage
      val promotionId = java.util.UUID.randomUUID().toString // Ensure promotionId is a valid non-null String
      val messageContent = KafkaMessage(
        actionType = "CREATE",
        parameters = Map(
          "promotionId" -> promotionId.asJson,
          "discount" -> 20.asJson,
          "validity" -> java.time.LocalDate.now().toString.asJson
        )
      )

      // Return the message content and metadata for Pact verification
      MessageAndMetadataBuilder(
        message = messageContent.asJson.noSpaces,
        metadata = Map("contentType" -> "application/json")
      )
  }

  // Define the test that sends the message and runs the verification
  test("pact verification for a promotion message") {
    verifyPacts() // This runs the verification against the pact file.
  }
}
