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

    Response response = invoke("POST", "/auth", authHeaders(), null);
    assertEquals("Lambda should respond to auth request", 200, response.code());

    String body = response.body().string();
    assertFalse("Should not have ClassNotFoundException", body.contains("ClassNotFoundException"));
    assertFalse("Should not have Python errors", body.contains("lambda_placeholder"));
  }

  @Test
  public void testLambdaJavaCodeIsExecuting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    Response response = invoke("GET", "/weekly-spot", new HashMap<>(), null);
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

    Response response = invoke("GET", "/me", authHeaders(), null);
    assertEquals("User endpoint should be routable", 200, response.code());
  }

  @Test
  public void testRsvpEndpointRouting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);
    Assume.assumeNotNull("Firebase token required for testing", FIREBASE_TOKEN);

    // Use a fake invite ID — we expect 404 (invite not found), not 401 (unauth) or 5xx (crash)
    Response response = invoke("POST", "/invites/smoke-test-invite/rsvp", authHeaders(), "{\"status\":\"yes\"}");
    int status = response.code();
    assertNotEquals("RSVP endpoint should not reject auth", 401, status);
    assertTrue("RSVP endpoint should not crash", status < 500);
  }

  @Test
  public void testPushTokenEndpointRouting() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);
    Assume.assumeNotNull("Firebase token required for testing", FIREBASE_TOKEN);

    String body = gson.toJson(Map.of("token", "smoke-test-fcm-token-placeholder", "platform", "ios"));
    Response response = invoke("POST", "/push-tokens", authHeaders(), body);
    assertEquals("Push tokens endpoint should be routable", 201, response.code());
  }

  @Test
  public void testWeeklyJobEndpointRequiresAuth() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    // Weekly job HTTP endpoint requires auth — verify it rejects unauthenticated requests
    Response response = invoke("POST", "/weekly-job", new HashMap<>(), null);
    assertEquals("Weekly job endpoint should require auth", 401, response.code());
  }

  @Test
  public void testInvalidPathReturns404() throws Exception {
    Assume.assumeNotNull("Lambda endpoint must be configured", LAMBDA_ENDPOINT);

    Response response = invoke("GET", "/invalid-path", new HashMap<>(), null);
    assertEquals("Invalid path should return 404", 404, response.code());
  }

  private Map<String, Object> authHeaders() {
    Map<String, Object> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + FIREBASE_TOKEN);
    return headers;
  }

  private Response invoke(String method, String path, Map<String, Object> headers, String bodyJson)
      throws Exception {
    String url = LAMBDA_ENDPOINT + path;
    Request.Builder builder = new Request.Builder().url(url);

    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      builder.addHeader(entry.getKey(), entry.getValue().toString());
    }

    String json = bodyJson != null ? bodyJson : "{}";
    RequestBody requestBody = RequestBody.create(json, okhttp3.MediaType.get("application/json"));

    Request request;
    switch (method) {
      case "POST":
        request = builder.post(requestBody).build();
        break;
      case "PATCH":
        request = builder.patch(requestBody).build();
        break;
      default:
        request = builder.get().build();
    }

    return client.newCall(request).execute();
  }
}
