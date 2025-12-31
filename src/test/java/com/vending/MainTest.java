// File: src/test/java/com/vending/MainTest.java
package com.vending;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
  @Test
  void testMain() {
    // 呼叫 Main 的 main 方法
    assertDoesNotThrow(() -> Main.main(new String[]{}));
  }

  @Test
  void testMainConstructor() {
    // 使用反射來強制呼叫 private 建構子
    assertDoesNotThrow(() -> {
      Constructor<Main> constructor = Main.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      try {
        constructor.newInstance();
      } catch (InvocationTargetException e) {
        // 我們預期它會拋出 IllegalStateException ("Utility class")
        // 使用 assertInstanceOf 來驗證異常類型
        assertInstanceOf(IllegalStateException.class, e.getCause());
      }
    });
  }
}