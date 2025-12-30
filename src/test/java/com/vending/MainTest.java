package com.vending;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
  @Test
  void testMain() {
    // 呼叫 Main 的 main 方法來覆蓋該類別的代碼
    assertDoesNotThrow(() -> Main.main(new String[]{}));
  }
  @Test
  void testMainConstructor() {
    // 單純為了覆蓋 Main 類別的定義
    new Main();
  }
}