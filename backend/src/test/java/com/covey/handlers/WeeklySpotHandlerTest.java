package com.covey.handlers;

import static org.junit.Assert.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WeeklySpotHandlerTest {
  private WeeklySpotHandler handler;

  @Mock
  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new WeeklySpotHandler();
  }

  @Test
  public void testHandleRequest() {
    JsonObject response = handler.handleRequest(new Object(), context);
    assertNotNull(response);
    assertEquals(200, response.get("statusCode").getAsInt());
  }
}
