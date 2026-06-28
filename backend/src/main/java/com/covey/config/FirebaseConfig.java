package com.covey.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class FirebaseConfig {
  private static FirebaseApp firebaseApp;

  public static synchronized void initialize() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      InputStream serviceAccount = loadCredentials();

      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .build();

      firebaseApp = FirebaseApp.initializeApp(options);
    }
  }

  private static InputStream loadCredentials() throws IOException {
    String secretName = System.getenv("FIREBASE_CREDENTIALS_SECRET");

    if (secretName != null && !secretName.isBlank()) {
      try {
        return loadFromSecretsManager(secretName);
      } catch (Exception e) {
        throw new IOException("Failed to load Firebase credentials from Secrets Manager: " + e.getMessage(), e);
      }
    }

    InputStream classpath = FirebaseConfig.class
        .getClassLoader()
        .getResourceAsStream("firebase-service-account.json");

    if (classpath != null) {
      return classpath;
    }

    throw new IOException(
        "Firebase credentials not found. Set FIREBASE_CREDENTIALS_SECRET environment variable or "
            + "place firebase-service-account.json in classpath");
  }

  private static InputStream loadFromSecretsManager(String secretName) {
    try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
      GetSecretValueRequest request = GetSecretValueRequest.builder()
          .secretId(secretName)
          .build();

      GetSecretValueResponse response = client.getSecretValue(request);
      String secretValue = response.secretString();

      return new ByteArrayInputStream(secretValue.getBytes());
    }
  }

  public static FirebaseAuth getAuth() throws IOException {
    initialize();
    return FirebaseAuth.getInstance();
  }
}
