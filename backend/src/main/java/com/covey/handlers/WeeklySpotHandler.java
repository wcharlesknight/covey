package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonObject;

public class WeeklySpotHandler implements RequestHandler<Object, JsonObject> {
  @Override
  public JsonObject handleRequest(Object input, Context context) {
    context.getLogger().log("WeeklySpotHandler invoked");

    JsonObject response = new JsonObject();
    response.addProperty("statusCode", 200);
    response.addProperty("message", "Weekly spot selection in progress");

    return response;
  }
}
