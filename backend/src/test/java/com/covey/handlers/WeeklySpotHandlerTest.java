package com.covey.handlers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;

public class WeeklySpotHandlerTest {
  private WeeklySpotHandler handler;
  private Context context;

  @Before
  public void setUp() {
    handler = new WeeklySpotHandler();
    context = mock(Context.class);
    when(context.getLogger()).thenReturn(System.out::println);
  }

  @Test
  public void testHandleRequest() {
    JsonObject response = handler.handleRequest(new Object(), context);
    assertNotNull(response);
    assertEquals(200, response.get("statusCode").getAsInt());
  }
}
