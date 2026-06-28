package com.covey.smoke;

import com.covey.config.FirebaseConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.io.IOException;

public class FirebaseTestTokenGenerator {

  public static String generateTestToken(String testUserId) throws FirebaseAuthException, IOException {
    FirebaseConfig.initialize();
    return FirebaseAuth.getInstance().createCustomToken(testUserId);
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: java FirebaseTestTokenGenerator <testUserId>");
      System.exit(1);
    }

    String testUserId = args[0];
    String token = generateTestToken(testUserId);
    System.out.println(token);
  }
}
