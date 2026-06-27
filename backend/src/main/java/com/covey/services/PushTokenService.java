package com.covey.services;

import com.covey.models.PushToken;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PushTokenService {
  private static final String PUSH_TOKENS_COLLECTION = "pushTokens";

  public String registerToken(String userId, String token, String platform)
      throws ExecutionException, InterruptedException {
    if (userId == null || userId.isEmpty()) {
      throw new IllegalArgumentException("User ID cannot be null or empty");
    }
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Token cannot be null or empty");
    }

    Firestore db = FirestoreClient.getFirestore();
    String tokenId = UUID.randomUUID().toString();
    PushToken pushToken = new PushToken(userId, token, platform);
    pushToken.setId(tokenId);

    db.collection(PUSH_TOKENS_COLLECTION).document(tokenId).set(pushToken).get();

    return tokenId;
  }

  public List<String> getActiveTokensByUser(String userId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();

    Query query = db.collection(PUSH_TOKENS_COLLECTION)
        .whereEqualTo("userId", userId)
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

  public void markTokenInactive(String tokenId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(PUSH_TOKENS_COLLECTION).document(tokenId)
        .update("isActive", false).get();
  }
}
