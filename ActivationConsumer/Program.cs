// Program.cs

using System;
using Confluent.Kafka;

namespace ActivationConsumer
{
    class Program
    {
        static void Main(string[] args)
        {
            var config = new ConsumerConfig
            {
                GroupId = "activation-consumer-group",
                BootstrapServers = "localhost:9092",
                AutoOffsetReset = AutoOffsetReset.Earliest
            };

            using(var consumer = new ConsumerBuilder<Ignore, string>(config).Build())
            {
                consumer.Subscribe("activation");

                try
                {
                    while (true)
                    {
                        var consumeResult = consumer.Consume();
                        Console.WriteLine($"Received message: {consumeResult.Message.Value}");

                        // Deserialize and process the message
                        // For example, parse the JSON and map to your internal model
                    }
                }
                catch (OperationCanceledException)
                {
                    consumer.Close();
                }
            }
        }
    }
}
