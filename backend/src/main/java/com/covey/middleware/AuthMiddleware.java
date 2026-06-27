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
      return Optional.empty();
    }

    String idToken = bearerToken.substring(7);

    try {
      FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
      String uid = decodedToken.getUid();
      return Optional.of(uid);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
