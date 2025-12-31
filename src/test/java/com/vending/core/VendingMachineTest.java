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
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VendingMachineTest - 終極覆蓋率版 (Target: >95%)
 */
class VendingMachineTest {
  private VendingMachine vm;

  @BeforeEach
  void setUp() { vm = new VendingMachine(); }

  // 1. 基礎測試
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
    assertEquals("T", d.getId()); d.setStock(5); assertEquals(5, d.getStock());
  }

  // 2. ChangeService 深度測試
  @Test
  void testChangeService_Physical() throws Exception {
    ChangeService cs = new ChangeService();
    Field d50 = ChangeService.class.getDeclaredField("diam50");
    d50.setAccessible(true); d50.setDouble(cs, 50.0);
    Map<Integer, Integer> res = cs.calculateChange(50);
    assertNull(res.get(50)); assertEquals(5, res.get(10));

    cs = new ChangeService();
    Field w50 = ChangeService.class.getDeclaredField("weight50");
    w50.setAccessible(true); w50.setDouble(cs, 20.0);
    res = cs.calculateChange(50);
    assertNull(res.get(50));

    cs = new ChangeService();
    w50.setDouble(cs, 8.0);
    res = cs.calculateChange(50);
    assertNull(res.get(50));

    Field d10 = ChangeService.class.getDeclaredField("diam10");
    d10.setAccessible(true); d10.setDouble(cs, 20.0);
    res = cs.calculateChange(10);
    assertNull(res.get(10));

    Field w1 = ChangeService.class.getDeclaredField("weight1");
    w1.setAccessible(true); w1.setDouble(cs, 6.0);
    assertFalse(cs.verifyCoinAuthenticity(1));
  }

  @Test
  void testChangeService_Audit() throws Exception {
    ChangeService cs = new ChangeService();
    cs.calculateChange(60);
    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);
    // ★ 關鍵：同時觸發所有庫存水位分支
    storage.put(50, 0);   // CRITICAL_EMPTY
    storage.put(10, 2);   // DANGER_LOW
    storage.put(5, 5);    // WARNING
    storage.put(1, 150);  // OVERFLOW
    cs.auditCoinReserves();
  }

  @Test
  void testChangeService_Edges() {
    ChangeService cs = new ChangeService();
    assertFalse(cs.verifyCoinAuthenticity(99));
    assertTrue(cs.verifyCoinAuthenticity(1));
    assertTrue(cs.calculateChange(0).isEmpty());
    assertTrue(cs.calculateChange(-5).isEmpty());
  }

  // 3. DiscountEngine 測試
  @Test
  void testDiscountEngine_AllBranches() {
    DiscountEngine de = new DiscountEngine();
    Drink d = new Drink("D", "D", 30, 10, false);

    // Non-VIP Logic (Score < 0) - 這就是修復後的測試目標
    assertEquals(25, de.applyPromotion(d, 101, false));

    // VIP Logic
    Drink vipD = new Drink("D", "D", 50, 10, false);
    assertEquals(39, de.applyPromotion(vipD, 101, true));

    // Luck Logic
    Drink lucky = new Drink("L1", "A_Lucky_8!", 77, 1, true);
    assertTrue(de.applyPromotion(lucky, 0, false) < 77);

    // Marketing
    Drink coffee = new Drink("C", "Latte Coffee", 50, 10, true);
    de.generateMarketingMessage(coffee);
    Drink tea = new Drink("T", "Ice Tea", 10, 10, false);
    de.generateMarketingMessage(tea);
    Drink soda = new Drink("S", "Coke Soda", 25, 10, false);
    de.generateMarketingMessage(soda);
    Drink nullName = new Drink("N", null, 20, 10, false);
    de.generateMarketingMessage(nullName);

    // Category
    de.applyPromotion(coffee, 0, true);
    de.applyPromotion(tea, 60, false);

    // Stock Logic
    Drink highStockHighPrice = new Drink("H", "H", 35, 20, false);
    assertEquals(30, de.applyPromotion(highStockHighPrice, 0, false));
    Drink highStockLowPrice = new Drink("L", "L", 20, 20, false);
    assertEquals(18, de.applyPromotion(highStockLowPrice, 0, false));
    Drink scarceTea = new Drink("T2", "Tea", 20, 2, false);
    assertEquals(18, de.applyPromotion(scarceTea, 60, false));
  }

  // 4. Maintenance & State
  @Test
  void testMaintenance_Report() {
    vm.getInventory().clear();
    vm.getInventory().put("D1", new Drink("D1", "Crit", 10, 0, true));
    vm.getInventory().put("D2", new Drink("D2", "Warn", 10, 5, true));
    vm.getInventory().put("D3", new Drink("D3", "Ok", 10, 10, false));
    vm.setState(vm.getMaintenanceState());
    vm.getMaintenanceState().maintenance("any");
  }

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

    ms.insertCoin(10);
    // 呼叫未知子系統，覆蓋 Switch Default
    Method checkMethod = MaintenanceState.class.getDeclaredMethod("performSubsystemCheck", String.class);
    checkMethod.setAccessible(true);
    checkMethod.invoke(ms, "UNKNOWN_SYSTEM");
  }

  @Test
  void testMaintenance_EdgeInputs() {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();
    ms.selectDrink(null); ms.selectDrink(""); ms.selectDrink("NON_EXIST_ID");
    vm.getInventory().put("A1", new Drink("A1", "Coke", 25, 5, false));
    ms.selectDrink("A1");
  }

  // 5. Core & State Machine (終極窮舉)
  @Test
  void testCoreAndStates() throws Exception {
    // --- 自我檢查循環測試 (針對每個 State 欄位) ---
    String[] stateFields = {"idleState", "hasMoneyState", "soldState", "soldOutState", "maintenanceState"};
    for (String fieldName : stateFields) {
      vm = new VendingMachine(); // 重置
      Field f = VendingMachine.class.getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(vm, null); // 破壞
      assertFalse(vm.performSystemSelfCheck(), "Field " + fieldName + " null check failed");
    }

    // --- Inventory 邏輯測試 ---
    vm = new VendingMachine();
    vm.getInventory().put("FREE", new Drink("F", "Free", 0, 10, false)); // 0元
    vm.getInventory().put("NEG", new Drink("N", "Neg", -1, 10, false)); // 負價
    vm.performSystemSelfCheck();
    assertEquals(0, vm.getInventory().get("NEG").getStock()); // 驗證修復

    // --- 餘額邏輯 ---
    vm.setBalance(2000); // 過高
    vm.performSystemSelfCheck();
    vm.setBalance(-10); // 負數
    vm.performSystemSelfCheck();
    assertEquals(0, vm.getBalance());

    // --- 狀態機流程 ---
    vm.setState(vm.getSoldState());
    vm.insertCoin(10); vm.selectDrink("A"); vm.cancel();
    vm.setCurrentDrink(new Drink("D","D",10,10,false));
    vm.setBalance(10);
    vm.dispense();

    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A"); vm.dispense(); vm.insertCoin(10);
    vm.enterMaintenance("wrong"); vm.enterMaintenance("admin123");

    vm.setState(vm.getIdleState());
    vm.insertCoin(1); vm.insertCoin(5); vm.insertCoin(10); vm.insertCoin(50);
    vm.insertCoin(3); vm.selectDrink("A"); vm.dispense(); vm.cancel();

    vm.setState(vm.getHasMoneyState());
    vm.insertCoin(10);
    vm.getInventory().put("A2", new Drink("A2", "Tea", 20, 0, false));
    vm.selectDrink("A2");
    assertInstanceOf(SoldOutState.class, vm.getCurrentState());

    vm.setState(vm.getHasMoneyState());
    vm.setBalance(5);
    vm.selectDrink("NON_EXIST");
    vm.selectDrink("A1");
  }
}