package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private AuthMiddleware authMiddleware;
  private FirebaseAuth firebaseAuth;
  private Gson gson;

  public AuthHandler() throws Exception {
    this.firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    String path = (String) event.getOrDefault("path", "");
    String method = (String) event.getOrDefault("httpMethod", "POST");

    // Strip stage prefix
    if (path.startsWith("/dev/")) {
      path = path.substring(4);
    } else if (path.startsWith("/prod/")) {
      path = path.substring(5);
    }

    context.getLogger().log("Auth request: " + method + " " + path);

    try {
      if (path.equals("/auth") && method.equals("POST")) {
        return handleTokenValidation(event, context);
      } else if (path.equals("/auth/refresh") && method.equals("POST")) {
        return handleTokenRefresh(event, context);
      } else {
        return notFound();
      }
    } catch (Exception e) {
      context.getLogger().log("Auth error: " + e.getMessage());
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 500);
      response.put("body", gson.toJson(Map.of("error", "Internal server error")));
      return response;
    }
  }

  /**
   * POST /auth — Validate Bearer token from Authorization header
   *
   * Request: Authorization: Bearer {idToken}
   * Response: { principalId: uid, status: "valid" }
   */
  private Map<String, Object> handleTokenValidation(Map<String, Object> event, Context context)
      throws Exception {
    context.getLogger().log("Validating token from Authorization header");

    String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
    Optional<String> uid = authMiddleware.validateToken(authHeader);

    if (uid.isPresent()) {
      context.getLogger().log("Token validated for user: " + uid.get());
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      Map<String, Object> body = new HashMap<>();
      body.put("principalId", uid.get());
      body.put("status", "valid");
      response.put("body", gson.toJson(body));
      return response;
    } else {
      context.getLogger().log("Token validation failed");
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 401);
      response.put("body", gson.toJson(Map.of("error", "Unauthorized")));
      return response;
    }
  }

  /**
   * POST /auth/refresh — Check if token needs refresh
   *
   * Request body: { "idToken": "current-firebase-id-token" }
   * Response: { "status": "valid|expired", "uid": "userId", "message": "Token is valid" }
   *
   * Note: Firebase Admin SDK cannot directly refresh ID tokens. Token refresh is handled
   * client-side via Firebase SDK. This endpoint validates the current token and advises
   * the client if refresh is needed (via status == "expired").
   */
  private Map<String, Object> handleTokenRefresh(Map<String, Object> event, Context context)
      throws Exception {
    context.getLogger().log("Checking token for refresh");

    // Parse request body
    String body = (String) event.getOrDefault("body", "{}");
    JsonObject requestBody = JsonParser.parseString(body).getAsJsonObject();
    String idToken = requestBody.has("idToken") ? requestBody.get("idToken").getAsString() : null;

    if (idToken == null || idToken.isEmpty()) {
      context.getLogger().log("Missing idToken in request body");
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 400);
      response.put("body", gson.toJson(Map.of("error", "Missing idToken")));
      return response;
    }

    try {
      // Verify the token (without checking revocation for performance)
      var decodedToken = firebaseAuth.verifyIdToken(idToken);
      String uid = decodedToken.getUid();

      context.getLogger().log("Token is valid for user: " + uid);

      // Token is valid; client should continue using it
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      Map<String, Object> responseBody = new HashMap<>();
      responseBody.put("status", "valid");
      responseBody.put("uid", uid);
      responseBody.put("message", "Token is still valid, no refresh needed");
      response.put("body", gson.toJson(responseBody));
      return response;

    } catch (FirebaseAuthException e) {
      context.getLogger().log("Token validation failed: " + e.getMessage());

      // Token is invalid or expired; client should refresh via Firebase SDK
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 401);
      Map<String, Object> responseBody = new HashMap<>();
      responseBody.put("status", "expired");
      responseBody.put("message", "Token is expired, client should refresh via Firebase SDK");
      response.put("body", gson.toJson(responseBody));
      return response;
    }
  }

  private Map<String, Object> notFound() {
    Map<String, Object> response = new HashMap<>();
    response.put("statusCode", 404);
    response.put("body", gson.toJson(Map.of("error", "Not Found")));
    return response;
  }
}
