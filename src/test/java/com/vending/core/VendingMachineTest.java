// File: src/test/java/com/vending/core/VendingMachineTest.java
package com.vending.core;

import com.vending.model.Drink;
import com.vending.service.ChangeService;
import com.vending.service.DiscountEngine;
import com.vending.state.*;
import com.vending.Main;

import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VendingMachineTest - 最終完美覆蓋版
 * 修正項目：找回遺失的邊界測試、狀態機無效輸入測試、庫存報告分支測試
 */
class VendingMachineTest {
  private VendingMachine vm;

  @BeforeEach
  void setUp() { vm = new VendingMachine(); }

  // =========================================================
  // 1. 基礎模型與 Main 類別
  // =========================================================
  @Test
  void testMainApp() {
    assertDoesNotThrow(() -> Main.main(new String[]{}));
    assertDoesNotThrow(() -> {
      Constructor<Main> c = Main.class.getDeclaredConstructor();
      c.setAccessible(true);
      try { c.newInstance(); }
      catch (InvocationTargetException e) { assertInstanceOf(IllegalStateException.class, e.getCause()); }
    });
  }

  @Test
  void testDrinkPojo() {
    Drink d = new Drink("T", "T", 10, 10, true);
    assertEquals("T", d.getId());
    assertEquals("T", d.getName());
    assertEquals(10, d.getPrice());
    assertEquals(10, d.getStock());
    assertTrue(d.isHot());
    d.setStock(5);
    assertEquals(5, d.getStock());
  }

  // =========================================================
  // 2. ChangeService: 窮舉所有偽幣與庫存狀態
  // =========================================================
  @Test
  void testChangeService_CoinProperties() throws Exception {
    ChangeService cs = new ChangeService();

    // 1. 直徑過大
    Field d50 = ChangeService.class.getDeclaredField("diam50");
    d50.setAccessible(true);
    d50.setDouble(cs, 50.0);
    Map<Integer, Integer> res = cs.calculateChange(50);
    assertNull(res.get(50));
    assertEquals(5, res.get(10));

    // 2. 重量過重
    cs = new ChangeService();
    Field w50 = ChangeService.class.getDeclaredField("weight50");
    w50.setAccessible(true);
    w50.setDouble(cs, 20.0);
    res = cs.calculateChange(50);
    assertNull(res.get(50));

    // 3. 材質檢測 (重量過輕)
    cs = new ChangeService();
    w50.setDouble(cs, 8.0);
    res = cs.calculateChange(50);
    assertNull(res.get(50));

    // 4. 材質檢測 (直徑過小) - 補強覆蓋率
    Field d10 = ChangeService.class.getDeclaredField("diam10");
    d10.setAccessible(true);
    d10.setDouble(cs, 20.0);
    res = cs.calculateChange(10);
    assertNull(res.get(10));
    assertEquals(2, res.get(5));

    // 5. 材質檢測 (Hash校驗)
    Field w1 = ChangeService.class.getDeclaredField("weight1");
    w1.setAccessible(true);
    w1.setDouble(cs, 6.0);
    assertFalse(cs.verifyCoinAuthenticity(1));
  }

  @Test
  void testChangeService_InventoryStates() throws Exception {
    ChangeService cs = new ChangeService();
    cs.calculateChange(60);

    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);

    // ★ 關鍵：同時觸發多種庫存狀態的 Log
    storage.put(50, 0);   // Critical
    storage.put(10, 2);   // Danger Low
    storage.put(5, 5);    // Warning
    storage.put(1, 150);  // Overflow

    cs.auditCoinReserves();
  }

  @Test
  void testChangeService_Edges() throws Exception {
    ChangeService cs = new ChangeService();
    assertFalse(cs.verifyCoinAuthenticity(99));
    assertTrue(cs.verifyCoinAuthenticity(1));

    // 強制清空某面額測試
    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);
    storage.put(10, 0);

    Map<Integer, Integer> res = cs.calculateChange(20);
    assertNull(res.get(10));
    assertEquals(4, res.get(5));

    // 負數輸入
    assertTrue(cs.calculateChange(0).isEmpty());
    assertTrue(cs.calculateChange(-5).isEmpty());
  }

  // =========================================================
  // 3. DiscountEngine: 完整邏輯覆蓋
  // =========================================================
  @Test
  void testDiscountEngine_AllBranches() {
    DiscountEngine de = new DiscountEngine();

    // 1. 邊界條件 (Balance > 100)
    Drink d = new Drink("D", "D", 30, 10, false);
    assertEquals(25, de.applyPromotion(d, 101, false));
    assertEquals(30, de.applyPromotion(d, 100, false));

    // 2. VIP測試 (Balance > 100) -> 39元
    Drink vipD = new Drink("D", "D", 50, 10, false);
    assertEquals(39, de.applyPromotion(vipD, 101, true));

    // 3. 幸運指數全滿
    Drink lucky = new Drink("L1", "A_Lucky_8!", 77, 1, true);
    int p = de.applyPromotion(lucky, 0, false);
    assertTrue(p < 77);

    // 4. 幸運指數低分
    Drink badLuck = new Drink("B", "B", 13, 5, false);
    de.applyPromotion(badLuck, 150, false);

    // 5. 行銷字串分支
    Drink coffee = new Drink("C", "Latte Coffee", 50, 10, true);
    de.generateMarketingMessage(coffee);
    Drink tea = new Drink("T", "Ice Tea", 10, 10, false);
    de.generateMarketingMessage(tea);
    Drink soda = new Drink("S", "Coke Soda", 25, 10, false);
    de.generateMarketingMessage(soda);

    // 6. 類別折扣
    de.applyPromotion(coffee, 0, true);
    de.applyPromotion(tea, 60, false);

    // 7. ★★★ 補回遺失的測試：庫存壓力分支 ★★★
    // Case A: 庫存多 (>15) 且 價格高 (>30) -> -5
    Drink highStockHighPrice = new Drink("H", "H", 35, 20, false);
    assertEquals(30, de.applyPromotion(highStockHighPrice, 0, false));

    // Case B: 庫存多 (>15) 但 價格低 (<=30) -> -2
    Drink highStockLowPrice = new Drink("L", "L", 20, 20, false);
    assertEquals(18, de.applyPromotion(highStockLowPrice, 0, false));

    // Case C: 稀缺保護
    Drink scarceTea = new Drink("T2", "Tea", 20, 2, false);
    assertEquals(18, de.applyPromotion(scarceTea, 60, false));
  }

  // =========================================================
  // 4. MaintenanceState: 感應器與輸入檢查
  // =========================================================
  @Test
  void testMaintenance_Sensors() throws Exception {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    Field tempF = MaintenanceState.class.getDeclaredField("currentTemp");
    Field voltF = MaintenanceState.class.getDeclaredField("currentVoltage");
    Field wifiF = MaintenanceState.class.getDeclaredField("wifi");
    Field simF = MaintenanceState.class.getDeclaredField("sim4g");

    tempF.setAccessible(true); voltF.setAccessible(true);
    wifiF.setAccessible(true); simF.setAccessible(true);

    // 溫度測試
    tempF.setInt(ms, 50); ms.dispense();
    tempF.setInt(ms, -5); ms.dispense();
    tempF.setInt(ms, 4);  ms.dispense();

    // 電壓測試
    voltF.setInt(ms, 220); ms.dispense();
    voltF.setInt(ms, 110); ms.dispense();

    // 網路測試
    wifiF.set(ms, true); simF.set(ms, true); ms.dispense();
    wifiF.set(ms, true); simF.set(ms, false); ms.dispense();
    wifiF.set(ms, false); simF.set(ms, true); ms.dispense();
    wifiF.set(ms, false); simF.set(ms, false); ms.dispense();

    // ★★★ 補回遺失的測試：庫存報告的「一般商品低庫存」分支 ★★★
    vm.getInventory().clear();
    vm.getInventory().put("D1", new Drink("D1", "缺貨", 10, 0, false)); // 0
    vm.getInventory().put("D2", new Drink("D2", "貴且少", 50, 2, false)); // <=3, >=30
    vm.getInventory().put("D3", new Drink("D3", "俗且少", 10, 2, false)); // <=3, <30 (這個之前漏了)
    vm.getInventory().put("D4", new Drink("D4", "正常", 10, 10, false)); // >3
    ms.maintenance("any");

    ms.insertCoin(10);
  }

  @Test
  void testMaintenance_EdgeInputs() {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    // ★★★ 補回遺失的測試：無效 ID 輸入 ★★★
    ms.selectDrink(null);
    ms.selectDrink("");
    ms.selectDrink("NON_EXIST_ID");
    ms.selectDrink("A1"); // 有效 ID (需先確保庫存中有 A1，或 mock)
    // 為了安全，重新塞入 A1
    vm.getInventory().put("A1", new Drink("A1", "Coke", 25, 5, false));
    ms.selectDrink("A1");
  }

  // =========================================================
  // 5. 狀態機與核心邏輯 (包含投幣窮舉)
  // =========================================================
  @Test
  void testCoreAndStates() throws Exception {
    // 自我檢查反射測試
    assertTrue(vm.performSystemSelfCheck());

    Field de = VendingMachine.class.getDeclaredField("discountEngine");
    de.setAccessible(true);
    de.set(vm, null);
    assertFalse(vm.performSystemSelfCheck());

    // 重置 VM
    vm = new VendingMachine();
    vm.setBalance(-10);
    vm.performSystemSelfCheck();
    assertEquals(0, vm.getBalance());

    // --- 狀態測試 ---

    // SoldState
    vm.setState(vm.getSoldState());
    vm.insertCoin(10); vm.selectDrink("A"); vm.cancel();
    vm.setCurrentDrink(new Drink("D","D",10,10,false));
    vm.setBalance(10);
    vm.dispense(); // 回到 Idle

    // SoldOutState
    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A"); vm.dispense();
    vm.insertCoin(10);
    vm.enterMaintenance("wrong");
    vm.enterMaintenance("admin123");

    // IdleState -> HasMoney (窮舉所有投幣可能，補足分支覆蓋)
    vm.setState(vm.getIdleState());
    vm.insertCoin(1);
    vm.insertCoin(5);
    vm.insertCoin(10);
    vm.insertCoin(50);
    vm.insertCoin(3); // 無效幣
    vm.selectDrink("A");
    vm.dispense();
    vm.cancel();

    // HasMoneyState (窮舉所有投幣可能)
    vm.setState(vm.getHasMoneyState());
    vm.insertCoin(1);
    vm.insertCoin(5);
    vm.insertCoin(10);
    vm.insertCoin(50);
    vm.insertCoin(3); // 無效幣

    // HasMoney -> SoldOut
    vm.getInventory().put("A2", new Drink("A2", "Tea", 20, 0, false));
    vm.selectDrink("A2");
    assertInstanceOf(SoldOutState.class, vm.getCurrentState());
  }
}