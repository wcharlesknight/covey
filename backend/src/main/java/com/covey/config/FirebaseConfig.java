package com.covey.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.io.InputStream;

public class FirebaseConfig {
  private static FirebaseApp firebaseApp;

  public static synchronized void initialize() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      InputStream serviceAccount = FirebaseConfig.class
          .getClassLoader()
          .getResourceAsStream("firebase-service-account.json");

      if (serviceAccount == null) {
        throw new IOException("firebase-service-account.json not found in classpath");
      }

      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .build();

      firebaseApp = FirebaseApp.initializeApp(options);
    }
  }

  public static FirebaseAuth getAuth() throws IOException {
    initialize();
    return FirebaseAuth.getInstance();
  }
}
