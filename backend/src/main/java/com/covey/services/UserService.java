package com.covey.services;

import com.covey.models.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class UserService {
  private static final String USERS_COLLECTION = "users";

  public Optional<User> getUser(String uid) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentReference docRef = db.collection(USERS_COLLECTION).document(uid);
    ApiFuture<DocumentSnapshot> future = docRef.get();
    DocumentSnapshot document = future.get();

    if (document.exists()) {
      return Optional.of(document.toObject(User.class));
    } else {
      return Optional.empty();
    }
  }

  public void saveUser(User user) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentReference docRef = db.collection(USERS_COLLECTION).document(user.getUid());
    user.setUpdatedAt(new java.util.Date().toInstant().toString());
    docRef.set(user).get();
  }

  public void createUser(User user) throws ExecutionException, InterruptedException {
    if (user.getUid() == null || user.getUid().isEmpty()) {
      throw new IllegalArgumentException("User UID cannot be null or empty");
    }
    saveUser(user);
  }
}
