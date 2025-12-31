// File: src/main/java/com/vending/Main.java
package com.vending;

import com.vending.core.VendingMachine;

public class Main {

  // ★★★ 新增：私有建構子，解決 "Utility class should not have public constructor" ★★★
  private Main() {
    throw new IllegalStateException("Utility class");
  }

  public static void main(String[] args) {
    VendingMachine vm = new VendingMachine();

    System.out.println("--- 測試場景 1：正常購買 ---");
    vm.insertCoin(10);
    vm.insertCoin(10);
    vm.insertCoin(10); // 餘額 30
    vm.selectDrink("A1"); // 可樂 25 元
    // 這裡會自動扣款並找零

    System.out.println("\n--- 測試場景 2：餘額不足 ---");
    vm.insertCoin(5);
    vm.selectDrink("A1"); // 應提示金額不足

    System.out.println("\n--- 測試場景 3：取消交易 ---");
    vm.cancel();

    System.out.println("\n--- 測試場景 4：進入維護模式 ---");
    vm.enterMaintenance("admin123");
    vm.selectDrink("A1"); // 維護模式下的補貨邏輯
    vm.cancel(); // 退出維護
  }
}