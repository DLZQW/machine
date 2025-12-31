package com.vending;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
  @Test
  void testMain() {
    assertDoesNotThrow(() -> Main.main(new String[]{}));
  }

  @Test
  void testMainConstructor() {
    assertDoesNotThrow(() -> {
      Constructor<Main> constructor = Main.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      try {
        constructor.newInstance();
      } catch (InvocationTargetException e) {
        assertInstanceOf(IllegalStateException.class, e.getCause());
      }
    });
  }
}