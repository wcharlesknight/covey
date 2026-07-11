package com.covey.middleware;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;

public class AuthMiddleware {
  private final FirebaseAuth firebaseAuth;

  public AuthMiddleware(FirebaseAuth firebaseAuth) {
    this.firebaseAuth = firebaseAuth;
  }

  public Optional<String> validateToken(String bearerToken) {
    if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
      System.err.println("❌ Invalid bearer token format");
      return Optional.empty();
    }

    String idToken = bearerToken.substring(7);

    try {
      FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
      String uid = decodedToken.getUid();
      System.out.println("✅ Token verified for user: " + uid);
      return Optional.of(uid);
    } catch (FirebaseAuthException e) {
      System.err.println("❌ Firebase auth error: " + e.getMessage());
      return Optional.empty();
    } catch (Exception e) {
      System.err.println("❌ Token verification failed: " + e.getMessage());
      return Optional.empty();
    }
  }
}
