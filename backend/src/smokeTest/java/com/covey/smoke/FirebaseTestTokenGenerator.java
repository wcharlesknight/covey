package com.covey.smoke;

import com.covey.config.FirebaseConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FirebaseTestTokenGenerator {

  public static String generateIdToken(String testUserId, String firebaseWebApiKey)
      throws FirebaseAuthException, IOException {
    FirebaseConfig.initialize();
    String customToken = FirebaseAuth.getInstance().createCustomToken(testUserId);
    return exchangeForIdToken(customToken, firebaseWebApiKey);
  }

  // Exchange a custom token for a Firebase ID token via the REST API.
  // Custom tokens (Admin SDK) cannot be used as Bearer tokens — they must be
  // exchanged first via signInWithCustomToken.
  private static String exchangeForIdToken(String customToken, String webApiKey) throws IOException {
    OkHttpClient client = new OkHttpClient();

    JsonObject body = new JsonObject();
    body.addProperty("token", customToken);
    body.addProperty("returnSecureToken", true);

    RequestBody requestBody = RequestBody.create(
        body.toString(), MediaType.get("application/json"));

    Request request = new Request.Builder()
        .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + webApiKey)
        .post(requestBody)
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body().string();
      if (!response.isSuccessful()) {
        throw new IOException("Token exchange failed (" + response.code() + "): " + responseBody);
      }
      JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
      return json.get("idToken").getAsString();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: java FirebaseTestTokenGenerator <testUserId>");
      System.exit(1);
    }

    String testUserId = args[0];
    String webApiKey = System.getenv("FIREBASE_WEB_API_KEY");
    if (webApiKey == null || webApiKey.isEmpty()) {
      System.err.println("FIREBASE_WEB_API_KEY env var required");
      System.exit(1);
    }

    String idToken = generateIdToken(testUserId, webApiKey);
    System.out.println(idToken);
  }
}
