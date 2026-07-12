package com.covey.services;

import com.covey.models.PushToken;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PushTokenService {

  /**
   * Register a push token for a user.
   *
   * Stores at: users/{userId}/pushTokens/{sha256(token)}
   * Uses SHA-256 hash of token as document ID for deterministic, idempotent writes.
   *
   * @param userId User ID (owner of token)
   * @param token Device token (e.g., APNs token, FCM registration token)
   * @param platform Platform (e.g., "ios", "android")
   * @return Deterministic token ID (SHA-256 hash)
   */
  public String registerToken(String userId, String token, String platform)
      throws ExecutionException, InterruptedException, NoSuchAlgorithmException {
    if (userId == null || userId.isEmpty()) {
      throw new IllegalArgumentException("User ID cannot be null or empty");
    }
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Token cannot be null or empty");
    }

    Firestore db = FirestoreClient.getFirestore();

    // Use SHA-256 hash of token as deterministic document ID
    String tokenId = hashToken(token);

    PushToken pushToken = new PushToken(userId, token, platform);
    pushToken.setId(tokenId);

    // Store in user-scoped subcollection: users/{userId}/pushTokens/{tokenId}
    db.collection("users")
        .document(userId)
        .collection("pushTokens")
        .document(tokenId)
        .set(pushToken)
        .get();

    return tokenId;
  }

  /**
   * Get all active push tokens for a user.
   *
   * Queries: users/{userId}/pushTokens where isActive == true
   *
   * @param userId User ID (owner)
   * @return List of device tokens
   */
  public List<String> getActiveTokensByUser(String userId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();

    // Query user's subcollection for active tokens
    Query query = db.collection("users")
        .document(userId)
        .collection("pushTokens")
        .whereEqualTo("isActive", true);

    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    List<String> tokens = new ArrayList<>();
    snapshot.getDocuments().forEach(doc -> {
      PushToken token = doc.toObject(PushToken.class);
      if (token != null) {
        tokens.add(token.getToken());
      }
    });

    return tokens;
  }

  /**
   * Mark a token as inactive (e.g., after APNs/FCM failure).
   *
   * @param userId User ID (owner)
   * @param tokenId Token ID (SHA-256 hash of device token)
   */
  public void markTokenInactive(String userId, String tokenId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection("users")
        .document(userId)
        .collection("pushTokens")
        .document(tokenId)
        .update("isActive", false)
        .get();
  }

  /**
   * Generate deterministic token ID as SHA-256 hash.
   *
   * @param token Raw device token
   * @return Hex-encoded SHA-256 hash
   */
  private String hashToken(String token) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
