package com.covey;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.handlers.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

public class LambdaRouter implements RequestHandler<Map<String, Object>, Map<String, Object>> {

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Lambda request received: " + event);

    try {
      String path = (String) event.getOrDefault("path", "");
      String method = (String) event.getOrDefault("httpMethod", "GET");

      context.getLogger().log("Path: " + path + ", Method: " + method);

      // Route to appropriate handler based on path
      if (path.equals("/weekly-job") && method.equals("POST")) {
        return new WeeklyJobHandler().handleRequest(event, context);
      } else if (path.equals("/me") && method.equals("GET")) {
        return new UserGetHandler().handleRequest(event, context);
      } else if (path.equals("/me") && method.equals("PATCH")) {
        return new UserPatchHandler().handleRequest(event, context);
      } else if (path.equals("/me/feed") && method.equals("GET")) {
        return new UserFeedHandler().handleRequest(event, context);
      } else if (path.matches("/invites/.*/rsvp") && method.equals("POST")) {
        return new RsvpHandler().handleRequest(event, context);
      } else if (path.equals("/push-tokens") && method.equals("POST")) {
        return new PushTokenHandler().handleRequest(event, context);
      } else if (path.equals("/weekly-spot") && method.equals("GET")) {
        return new WeeklySpotHandler().handleRequest(event, context);
      } else if (path.equals("/auth") && method.equals("POST")) {
        return new AuthHandler().handleRequest(event, context);
      } else {
        // 404 Not Found
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("statusCode", 404);
        response.put("body", "Not Found");
        return response;
      }
    } catch (Exception e) {
      context.getLogger().log("Error: " + e.getMessage());
      Map<String, Object> response = new java.util.HashMap<>();
      response.put("statusCode", 500);
      response.put("body", "Internal Server Error: " + e.getMessage());
      return response;
    }
  }
}
