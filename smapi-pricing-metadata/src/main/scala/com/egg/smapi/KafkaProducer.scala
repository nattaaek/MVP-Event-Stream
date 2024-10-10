package com.egg.smapi

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import java.util.Properties
import org.apache.kafka.common.serialization.StringSerializer
import io.circe.syntax._
import io.circe.generic.auto._
import com.egg.smapi.KafkaMiddleware.KafkaMessage

object KafkaProducerApp {
  import io.circe.syntax._

  private val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)
  props.put("acks", "all")

  private val producer = new KafkaProducer[String, String](props)

  // Method to send a generic KafkaMessage in a blocking manner
  def sendMessage(topic: String, message: KafkaMessage, key: String): Either[Exception, RecordMetadata] = {
    // Serialize the message to JSON
    val messageJson = message.asJson.noSpaces

    // Create a ProducerRecord
    val record = new ProducerRecord[String, String](topic, key, messageJson)

    try {
      // Send the message and wait for the result synchronously
      val metadata = producer.send(record).get() // This blocks until the message is acknowledged
      Right(metadata)
    } catch {
      case e: Exception => Left(e)
    }
  }

  def close(): Unit = {
    producer.close()
  }
}
