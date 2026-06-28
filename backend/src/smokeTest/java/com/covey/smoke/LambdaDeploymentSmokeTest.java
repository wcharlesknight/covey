package com.covey.smoke;

import static org.junit.Assert.*;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Assume;
import org.junit.Test;

public class LambdaDeploymentSmokeTest {

  private static final String LAMBDA_ENDPOINT = System.getenv("LAMBDA_ENDPOINT");
  private static final String FIREBASE_TOKEN = System.getenv("FIREBASE_TEST_TOKEN");
  private static final OkHttpClient client = new OkHttpClient();
  private static final Gson gson = new Gson();

  @Test
  public void testLambdaIsRespondingToAuthRequest() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);
    Assume.assumeNotNull("Firebase token required for testing", FIREBASE_TOKEN);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/auth");
    payload.put("httpMethod", "POST");
    payload.put("authorizationToken", "Bearer " + FIREBASE_TOKEN);

    Response response = invokeLambda(payload);
    assertEquals("Lambda should respond to auth request", 200, response.code());

    String body = response.body().string();
    assertFalse("Should not have ClassNotFoundException", body.contains("ClassNotFoundException"));
    assertFalse("Should not have Python errors", body.contains("lambda_placeholder"));
  }

  @Test
  public void testLambdaJavaCodeIsExecuting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/weekly-spot");
    payload.put("httpMethod", "GET");

    Response response = invokeLambda(payload);
    String body = response.body().string();

    assertFalse("Should execute Java code (no Python errors)",
      body.contains("lambda_placeholder") || body.contains("ImportError"));
    assertFalse("Should not have missing class errors",
      body.contains("ClassNotFoundException"));
  }

  @Test
  public void testUserEndpointRouting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);
    Assume.assumeNotNull("Firebase token required for testing", FIREBASE_TOKEN);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/me");
    payload.put("httpMethod", "GET");
    payload.put("authorizationToken", "Bearer " + FIREBASE_TOKEN);

    Response response = invokeLambda(payload);
    assertEquals("User endpoint should be routable", 200, response.code());
  }

  @Test
  public void testRsvpEndpointRouting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);
    Assume.assumeNotNull("Firebase token required for testing", FIREBASE_TOKEN);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/invites/test-id/rsvp");
    payload.put("httpMethod", "POST");
    payload.put("authorizationToken", "Bearer " + FIREBASE_TOKEN);

    Response response = invokeLambda(payload);
    assertEquals("RSVP endpoint should be routable", 200, response.code());
  }

  @Test
  public void testPushTokenEndpointRouting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/push-tokens");
    payload.put("httpMethod", "POST");

    Response response = invokeLambda(payload);
    assertEquals("Push tokens endpoint should be routable", 200, response.code());
  }

  @Test
  public void testWeeklyJobEndpointRouting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/weekly-job");
    payload.put("httpMethod", "POST");

    Response response = invokeLambda(payload);
    assertEquals("Weekly job endpoint should be routable", 200, response.code());
  }

  @Test
  public void testInvalidPathReturns404() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    Map<String, Object> payload = new HashMap<>();
    payload.put("path", "/invalid-path");
    payload.put("httpMethod", "GET");

    Response response = invokeLambda(payload);
    assertEquals("Invalid path should return 404", 404, response.code());
  }

  private Response invokeLambda(Map<String, Object> payload) throws Exception {
    String jsonPayload = gson.toJson(payload);

    RequestBody body = RequestBody.create(
      jsonPayload,
      okhttp3.MediaType.get("application/json")
    );

    Request request = new Request.Builder()
      .url(LAMBDA_ENDPOINT)
      .post(body)
      .build();

    return client.newCall(request).execute();
  }
}
