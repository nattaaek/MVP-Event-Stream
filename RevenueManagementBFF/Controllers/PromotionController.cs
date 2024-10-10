// Controllers/PromotionController.cs

using Microsoft.AspNetCore.Mvc;
using GraphQL;
using GraphQL.Client.Http;
using GraphQL.Client.Serializer.Newtonsoft;

[ApiController]
[Route("api/[controller]")]
public class PromotionController : ControllerBase
{
    private readonly GraphQLHttpClient _client;

    public PromotionController()
    {
        _client = new GraphQLHttpClient("http://localhost:5000/graphql", new NewtonsoftJsonSerializer());
    }

    [HttpPost("create-promotion")]
    public async Task<IActionResult> CreatePromotion([FromBody] PromotionInput input)
    {
        var mutation = new GraphQLRequest
        {
            Query = @"
                mutation($promotionId: ID!, $actionTypeId: String!, $parameters: String!) {
                    createPromotion(promotionId: $promotionId, actionTypeId: $actionTypeId, parameters: $parameters) {
                        success
                        message
                    }
                }",
            Variables = input
        };

        var response = await _client.SendMutationAsync<dynamic>(mutation);
        return Ok(response.Data);
    }
}

public class PromotionInput
{
    public string PromotionId { get; set; }
    public string ActionTypeId { get; set; }
    public string Parameters { get; set; }
}
