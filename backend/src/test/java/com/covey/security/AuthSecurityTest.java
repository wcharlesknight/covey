package com.covey.security;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.covey.middleware.AuthMiddleware;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class AuthSecurityTest {
  private FirebaseAuth firebaseAuth;
  private AuthMiddleware authMiddleware;

  @Before
  public void setUp() {
    firebaseAuth = mock(FirebaseAuth.class);
    authMiddleware = new AuthMiddleware(firebaseAuth);
  }

  @Test
  public void testAuthRejectsMissingBearerPrefix() {
    Optional<String> result = authMiddleware.validateToken("invalid_token_no_bearer");
    assertFalse(result.isPresent());
  }

  @Test
  public void testAuthRejectsEmptyToken() {
    Optional<String> result = authMiddleware.validateToken("");
    assertFalse(result.isPresent());
  }

  @Test
  public void testAuthRejectsNullToken() {
    Optional<String> result = authMiddleware.validateToken(null);
    assertFalse(result.isPresent());
  }

  @Test
  public void testAuthRejectsExpiredToken() throws FirebaseAuthException {
    String expiredToken = "Bearer expired.token.here";

    when(firebaseAuth.verifyIdToken(anyString()))
        .thenThrow(new RuntimeException("Token expired"));

    Optional<String> result = authMiddleware.validateToken(expiredToken);
    assertFalse(result.isPresent());
  }

  @Test
  public void testAuthRejectsInvalidSignature() throws FirebaseAuthException {
    String invalidToken = "Bearer invalid.signature.here";

    when(firebaseAuth.verifyIdToken(anyString()))
        .thenThrow(new RuntimeException("Invalid signature"));

    Optional<String> result = authMiddleware.validateToken(invalidToken);
    assertFalse(result.isPresent());
  }

  @Test
  public void testAuthExtractsUidFromValidToken() throws FirebaseAuthException {
    String validToken = "Bearer valid.token.here";
    String expectedUid = "user-12345";

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(mockToken.getUid()).thenReturn(expectedUid);
    when(firebaseAuth.verifyIdToken(anyString())).thenReturn(mockToken);

    Optional<String> result = authMiddleware.validateToken(validToken);

    assertTrue(result.isPresent());
    assertEquals(expectedUid, result.get());
  }
}
