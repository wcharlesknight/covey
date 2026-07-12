package com.covey;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.handlers.*;
import java.util.Map;

public class LambdaRouter implements RequestHandler<Map<String, Object>, Map<String, Object>> {

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Lambda request received: " + event);

    try {
      // EventBridge scheduled events have a triggerType field at the top level
      if (event.containsKey("triggerType")) {
        return handleScheduledEvent((String) event.get("triggerType"), event, context);
      }

      String path = (String) event.getOrDefault("path", "");
      String method = (String) event.getOrDefault("httpMethod", "GET");

      // Strip stage prefix from path (e.g., /dev/me/feed -> /me/feed)
      if (path.startsWith("/dev/")) {
        path = path.substring(4);
      } else if (path.startsWith("/prod/")) {
        path = path.substring(5);
      }

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
        Object result = new WeeklySpotHandler().handleRequest(event, context);
        if (result instanceof Map) {
          return (Map<String, Object>) result;
        } else {
          // Convert JsonObject to Map if needed
          Map<String, Object> response = new java.util.HashMap<>();
          response.put("statusCode", 200);
          response.put("body", result.toString());
          return response;
        }
      } else if ((path.equals("/auth") || path.equals("/auth/refresh")) && method.equals("POST")) {
        return new AuthHandler().handleRequest(event, context);
      } else if (path.equals("/notifications/send-test") && method.equals("POST")) {
        return new NotificationTriggerHandler().handleRequest(event, context);
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

  private Map<String, Object> handleScheduledEvent(String triggerType, Map<String, Object> event, Context context) {
    context.getLogger().log("Scheduled event: triggerType=" + triggerType);
    try {
      switch (triggerType) {
        case "WEEKLY_SELECTION":
          return new WeeklyJobHandler().handleScheduledEvent(context);
        case "NOTIFICATION_DELIVERY":
          return new NotificationDispatchHandler().handleRequest(event, context);
        default:
          context.getLogger().log("Unknown triggerType: " + triggerType);
          Map<String, Object> response = new java.util.HashMap<>();
          response.put("statusCode", 400);
          response.put("body", "Unknown triggerType: " + triggerType);
          return response;
      }
    } catch (Exception e) {
      context.getLogger().log("Scheduled event failed: " + e.getMessage());
      Map<String, Object> response = new java.util.HashMap<>();
      response.put("statusCode", 500);
      response.put("body", "Scheduled event failed: " + e.getMessage());
      return response;
    }
  }
}
