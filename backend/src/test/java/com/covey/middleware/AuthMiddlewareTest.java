package com.covey.middleware;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class AuthMiddlewareTest {
  private FirebaseAuth firebaseAuth;
  private AuthMiddleware authMiddleware;

  @Before
  public void setUp() {
    firebaseAuth = mock(FirebaseAuth.class);
    authMiddleware = new AuthMiddleware(firebaseAuth);
  }

  @Test
  public void testValidToken() throws FirebaseAuthException {
    String validToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    String expectedUid = "user123";

    FirebaseToken mockToken = mock(FirebaseToken.class);
    when(mockToken.getUid()).thenReturn(expectedUid);
    when(firebaseAuth.verifyIdToken(anyString())).thenReturn(mockToken);

    Optional<String> result = authMiddleware.validateToken(validToken);

    assertTrue(result.isPresent());
    assertEquals(expectedUid, result.get());
  }

  @Test
  public void testInvalidToken() throws FirebaseAuthException {
    String invalidToken = "Bearer invalid.token.here";

    when(firebaseAuth.verifyIdToken(anyString()))
        .thenThrow(new RuntimeException("Invalid token"));

    Optional<String> result = authMiddleware.validateToken(invalidToken);

    assertFalse(result.isPresent());
  }

  @Test
  public void testMissingBearerPrefix() {
    String tokenWithoutBearer = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

    Optional<String> result = authMiddleware.validateToken(tokenWithoutBearer);

    assertFalse(result.isPresent());
  }

  @Test
  public void testNullToken() {
    Optional<String> result = authMiddleware.validateToken(null);

    assertFalse(result.isPresent());
  }

  @Test
  public void testEmptyToken() {
    Optional<String> result = authMiddleware.validateToken("");

    assertFalse(result.isPresent());
  }
}
