using PactNet;
using PactNet.Matchers;
using Xunit;
using System.Text.Json;

namespace ActivationConsumer.Tests
{
    public class KafkaConsumerPactTest
    {
        private readonly IMessagePactBuilderV4 messagePact;

        public KafkaConsumerPactTest()
        {
            var pactConfig = new PactConfig
            {
                PactDir = "../../../pacts/",
                DefaultJsonSettings = new JsonSerializerOptions
                {
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase
                }
            };

            // Create a Pact V4 instance
            var pact = Pact.V4("ActivationConsumer", "SMAPIPricingMetadata", pactConfig);

            // Use WithMessageInteractions() to initiate message interactions
            this.messagePact = pact.WithMessageInteractions();
        }

        [Fact]
        public void EnsureConsumerCanDeserializeProducerMessages()
        {
            this.messagePact
                .ExpectsToReceive("a promotion message")
                .Given("Promotion data is available")
                .WithMetadata("contentType", "application/json")
                .WithJsonContent(new
                {
                    promotionId = Match.Regex(
                        "123e4567-e89b-12d3-a456-426614174000",
                        @"^[\da-f]{8}-([\da-f]{4}-){3}[\da-f]{12}$"
                    ),
                    actionTypeId = Match.Type("CREATE"),
                    parameters = new
                    {
                        discount = Match.Type(20),
                        validity = Match.Regex(
                            "2023-12-31",
                            @"^\d{4}-\d{2}-\d{2}$"
                        )
                    }
                })
                .Verify<MessageContent>(messageContent =>
                {
                    // Verification logic
                    Assert.NotNull(messageContent);
                    Assert.Equal("123e4567-e89b-12d3-a456-426614174000", messageContent.PromotionId);
                    Assert.Equal("CREATE", messageContent.ActionTypeId);
                    Assert.Equal(20, messageContent.Parameters.Discount);
                    Assert.Equal("2023-12-31", messageContent.Parameters.Validity);
                });
        }

        // Define your data models for deserialization
        public class MessageContent
        {
            public string PromotionId { get; set; }
            public string ActionTypeId { get; set; }
            public Parameters Parameters { get; set; }
        }

        public class Parameters
        {
            public int Discount { get; set; }
            public string Validity { get; set; }
        }
    }
}
