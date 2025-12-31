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
import java.lang.reflect.Method; // 新增 Method 反射
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VendingMachineTest - 最終極致版 (Target: >95%)
 * 包含：核心邏輯、反射破壞、邊界窮舉、死角清除
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
  // 2. ChangeService: 物理特性反射與庫存狀態
  // =========================================================
  @Test
  void testChangeService_CoinProperties() throws Exception {
    ChangeService cs = new ChangeService();

    // 1. 直徑過大 (50元)
    Field d50 = ChangeService.class.getDeclaredField("diam50");
    d50.setAccessible(true);
    d50.setDouble(cs, 50.0);
    Map<Integer, Integer> res = cs.calculateChange(50);
    assertNull(res.get(50));
    assertEquals(5, res.get(10));

    // 2. 重量過重 (50元 Tolerance)
    cs = new ChangeService();
    Field w50 = ChangeService.class.getDeclaredField("weight50");
    w50.setAccessible(true);
    w50.setDouble(cs, 20.0);
    res = cs.calculateChange(50);
    assertNull(res.get(50));

    // 3. 材質檢測 (重量過輕 Mat 1)
    cs = new ChangeService();
    w50.setDouble(cs, 8.0);
    res = cs.calculateChange(50);
    assertNull(res.get(50));

    // 4. 材質檢測 (直徑過小 Mat 2)
    Field d10 = ChangeService.class.getDeclaredField("diam10");
    d10.setAccessible(true);
    d10.setDouble(cs, 20.0);
    res = cs.calculateChange(10);
    assertNull(res.get(10));
    assertEquals(2, res.get(5));

    // 5. 材質檢測 (重量過重 Mat 3)
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

    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);
    storage.put(10, 0);

    Map<Integer, Integer> res = cs.calculateChange(20);
    assertNull(res.get(10));
    assertEquals(4, res.get(5));

    assertTrue(cs.calculateChange(0).isEmpty());
    assertTrue(cs.calculateChange(-5).isEmpty());
  }

  // =========================================================
  // 3. DiscountEngine: 完整邏輯覆蓋 (含 Null Name)
  // =========================================================
  @Test
  void testDiscountEngine_AllBranches() {
    DiscountEngine de = new DiscountEngine();

    // 1. 邊界條件
    Drink d = new Drink("D", "D", 30, 10, false);
    assertEquals(25, de.applyPromotion(d, 101, false));
    assertEquals(30, de.applyPromotion(d, 100, false));

    // 2. VIP測試 (修正預期值為 39)
    Drink vipD = new Drink("D", "D", 50, 10, false);
    assertEquals(39, de.applyPromotion(vipD, 101, true));

    // 3. 幸運指數
    Drink lucky = new Drink("L1", "A_Lucky_8!", 77, 1, true);
    int p = de.applyPromotion(lucky, 0, false);
    assertTrue(p < 77);

    Drink badLuck = new Drink("B", "B", 13, 5, false);
    de.applyPromotion(badLuck, 150, false);

    // 4. 行銷字串
    Drink coffee = new Drink("C", "Latte Coffee", 50, 10, true);
    de.generateMarketingMessage(coffee);
    Drink tea = new Drink("T", "Ice Tea", 10, 10, false);
    de.generateMarketingMessage(tea);
    Drink soda = new Drink("S", "Coke Soda", 25, 10, false);
    de.generateMarketingMessage(soda);

    de.applyPromotion(coffee, 0, true);
    de.applyPromotion(tea, 60, false);

    // 5. 庫存壓力
    Drink highStockHighPrice = new Drink("H", "H", 35, 20, false);
    assertEquals(30, de.applyPromotion(highStockHighPrice, 0, false));

    Drink highStockLowPrice = new Drink("L", "L", 20, 20, false);
    assertEquals(18, de.applyPromotion(highStockLowPrice, 0, false));

    Drink scarceTea = new Drink("T2", "Tea", 20, 2, false);
    assertEquals(18, de.applyPromotion(scarceTea, 60, false));

    // ★★★ 補強：名稱為 Null 的情況 (覆蓋 determineCategory UNKNOWN) ★★★
    Drink nullNameDrink = new Drink("N", null, 20, 10, false);
    de.applyPromotion(nullNameDrink, 0, false); // 不應拋出 NPE
  }

  // =========================================================
  // 4. MaintenanceState: 感應器與未知子系統
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

    tempF.setInt(ms, 50); ms.dispense();
    tempF.setInt(ms, -5); ms.dispense();
    tempF.setInt(ms, 4);  ms.dispense();

    voltF.setInt(ms, 220); ms.dispense();
    voltF.setInt(ms, 110); ms.dispense();

    wifiF.set(ms, true); simF.set(ms, true); ms.dispense();
    wifiF.set(ms, true); simF.set(ms, false); ms.dispense();
    wifiF.set(ms, false); simF.set(ms, true); ms.dispense();
    wifiF.set(ms, false); simF.set(ms, false); ms.dispense();

    vm.getInventory().put("D1", new Drink("D1", "E", 10, 0, false));
    vm.getInventory().put("D2", new Drink("D2", "L", 50, 2, false));
    ms.maintenance("any");

    ms.insertCoin(10);

    // ★★★ 補強：測試未知子系統 (覆蓋 Switch Default) ★★★
    Method checkMethod = MaintenanceState.class.getDeclaredMethod("performSubsystemCheck", String.class);
    checkMethod.setAccessible(true);
    checkMethod.invoke(ms, "UNKNOWN_SYSTEM");
  }

  @Test
  void testMaintenance_EdgeInputs() {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    ms.selectDrink(null);
    ms.selectDrink("");
    ms.selectDrink("NON_EXIST_ID");
    vm.getInventory().put("A1", new Drink("A1", "Coke", 25, 5, false));
    ms.selectDrink("A1");
  }

  // =========================================================
  // 5. 狀態機與核心邏輯 (含餘額過高、0元商品)
  // =========================================================
  @Test
  void testCoreAndStates() throws Exception {
    assertTrue(vm.performSystemSelfCheck());

    Field de = VendingMachine.class.getDeclaredField("discountEngine");
    de.setAccessible(true);
    de.set(vm, null);
    assertFalse(vm.performSystemSelfCheck());

    vm = new VendingMachine(); // 重置

    // ★★★ 補強：測試餘額過高異常與0元商品 ★★★
    vm.setBalance(2000); // > 1000
    vm.getInventory().put("FREE", new Drink("F", "Free", 0, 10, false)); // 0元
    vm.performSystemSelfCheck(); // 觸發警告 Log

    vm.setBalance(-10); // < 0
    vm.performSystemSelfCheck();
    assertEquals(0, vm.getBalance());

    // --- 狀態測試 ---
    vm.setState(vm.getSoldState());
    vm.insertCoin(10); vm.selectDrink("A"); vm.cancel();
    vm.setCurrentDrink(new Drink("D","D",10,10,false));
    vm.setBalance(10);
    vm.dispense();

    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A"); vm.dispense();
    vm.insertCoin(10);
    vm.enterMaintenance("wrong");
    vm.enterMaintenance("admin123");

    // Idle -> HasMoney
    vm.setState(vm.getIdleState());
    vm.insertCoin(1);
    vm.insertCoin(5);
    vm.insertCoin(10);
    vm.insertCoin(50);
    vm.insertCoin(3); // 無效幣
    vm.selectDrink("A");
    vm.dispense();
    vm.cancel();

    // HasMoney
    vm.setState(vm.getHasMoneyState());
    vm.insertCoin(10);

    // HasMoney -> SoldOut
    vm.getInventory().put("A2", new Drink("A2", "Tea", 20, 0, false));
    vm.selectDrink("A2");
    assertInstanceOf(SoldOutState.class, vm.getCurrentState());

    // ★★★ 補強：HasMoneyState 無此商品與餘額不足 ★★★
    vm.setState(vm.getHasMoneyState());
    vm.setBalance(5);
    vm.selectDrink("NON_EXIST"); // 無此商品
    vm.selectDrink("A1"); // 餘額不足 (A1=25, Bal=5)
  }
}