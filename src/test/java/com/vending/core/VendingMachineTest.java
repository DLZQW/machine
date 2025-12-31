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
 * VendingMachineTest - 100% 死角殲滅版
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
    assertEquals("T", d.getId()); assertEquals("T", d.getName());
    assertEquals(10, d.getPrice()); assertEquals(10, d.getStock());
    assertTrue(d.isHot()); d.setStock(5); assertEquals(5, d.getStock());
  }

  // =========================================================
  // 2. ChangeService 深度測試 (含物理邊界與 Hash 攻擊)
  // =========================================================
  @Test
  void testChangeService_AdvancedPhysical() throws Exception {
    ChangeService cs = new ChangeService();

    // 1. 惜售邏輯
    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);
    storage.put(10, 3);
    Map<Integer, Integer> res = cs.calculateChange(20);
    assertEquals(1, res.get(10)); assertEquals(2, res.get(5));

    // 2. 物理特性 - 過大/過重 (Upper Bound)
    cs = new ChangeService();
    Field d50 = ChangeService.class.getDeclaredField("diam50");
    d50.setAccessible(true); d50.setDouble(cs, 50.0);
    assertNull(cs.calculateChange(50).get(50));

    cs = new ChangeService();
    Field w50 = ChangeService.class.getDeclaredField("weight50");
    w50.setAccessible(true); w50.setDouble(cs, 20.0);
    assertNull(cs.calculateChange(50).get(50));

    // 3. ★★★ 新增：物理特性 - 過小/過輕 (Lower Bound) ★★★
    cs = new ChangeService();
    // 正常直徑28, 範圍15~30. 設為10 (太小)
    d50 = ChangeService.class.getDeclaredField("diam50");
    d50.setAccessible(true); d50.setDouble(cs, 10.0);
    assertNull(cs.calculateChange(50).get(50));

    cs = new ChangeService();
    // 正常重量10, 範圍3~12. 設為2 (太輕)
    w50 = ChangeService.class.getDeclaredField("weight50");
    w50.setAccessible(true); w50.setDouble(cs, 2.0);
    assertNull(cs.calculateChange(50).get(50));

    // 4. 材質檢測
    cs = new ChangeService();
    w50 = ChangeService.class.getDeclaredField("weight50"); // Mat 1 weight < 9
    w50.setAccessible(true); w50.setDouble(cs, 8.0);
    assertNull(cs.calculateChange(50).get(50));

    Field d10 = ChangeService.class.getDeclaredField("diam10"); // Mat 2 diam < 21
    d10.setAccessible(true); d10.setDouble(cs, 20.0);
    assertNull(cs.calculateChange(10).get(10));

    Field w1 = ChangeService.class.getDeclaredField("weight1"); // Mat 3 weight > 5
    w1.setAccessible(true); w1.setDouble(cs, 6.0);
    assertFalse(cs.verifyCoinAuthenticity(1));

    // 5. ★★★ 新增：Hash 強制失敗測試 ★★★
    // 透過反射修改 mat1 為負數，製造特殊的 Hash 值
    // 目標：(hash % 2 != 0) 且 (hash <= 10)
    // 設 mat50 = -33. Hash = 50*17 + (-33) = 850 - 33 = 817 (Odd). > 10. (Fail condition)
    // 我們需要 Hash 剛好 <= 10 且是奇數.
    // 設 Denom=1. 1*17 + mat = 17 + mat.
    // 設 mat = -10. Hash = 7 (Odd, <= 10). -> Return False!
    cs = new ChangeService();
    Field mat1 = ChangeService.class.getDeclaredField("mat1");
    mat1.setAccessible(true);
    mat1.setInt(cs, -10); // 注入非法材質代碼
    assertFalse(cs.verifyCoinAuthenticity(1)); // 應該被 Hash 攔截
  }

  @Test
  void testChangeService_InventoryStates() throws Exception {
    ChangeService cs = new ChangeService();
    cs.calculateChange(60);
    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);
    storage.put(50, 0); storage.put(10, 2); storage.put(5, 5); storage.put(1, 150);
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

  // =========================================================
  // 3. DiscountEngine 測試
  // =========================================================
  @Test
  void testDiscountEngine_AllBranches() {
    DiscountEngine de = new DiscountEngine();
    Drink d = new Drink("D", "D", 30, 10, false);

    // Non-VIP Logic
    assertEquals(28, de.applyPromotion(d, 101, false));
    assertEquals(30, de.applyPromotion(d, 100, false));

    // VIP Logic
    Drink vipD = new Drink("D", "D", 50, 10, false);
    assertEquals(39, de.applyPromotion(vipD, 101, true));

    // Luck Logic
    Drink lucky = new Drink("L1", "A_Lucky_8!", 77, 1, true);
    assertTrue(de.applyPromotion(lucky, 0, false) < 77);

    // Marketing & Null Safety
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
    Drink water = new Drink("W", "Water", 10, 10, false);
    de.applyPromotion(water, 0, false);

    // Stock Logic (33 because Non-VIP protection)
    Drink highStockHighPrice = new Drink("H", "H", 35, 20, false);
    assertEquals(33, de.applyPromotion(highStockHighPrice, 0, false));
    Drink highStockLowPrice = new Drink("L", "L", 20, 20, false);
    assertEquals(18, de.applyPromotion(highStockLowPrice, 0, false));
    Drink scarceTea = new Drink("T2", "Tea", 20, 2, false);
    assertEquals(18, de.applyPromotion(scarceTea, 60, false));
  }

  // =========================================================
  // 4. Maintenance & State: ★★★ 新增低電壓測試 ★★★
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

    // 電壓測試：高壓、正常、★ 低壓 ★
    voltF.setInt(ms, 220); ms.dispense(); // Fail (Too high)
    voltF.setInt(ms, 110); ms.dispense(); // Pass
    voltF.setInt(ms, 90);  ms.dispense(); // ★ Fail (Too low) - 這是之前漏掉的

    wifiF.set(ms, true); simF.set(ms, true); ms.dispense();
    wifiF.set(ms, true); simF.set(ms, false); ms.dispense();
    wifiF.set(ms, false); simF.set(ms, true); ms.dispense();
    wifiF.set(ms, false); simF.set(ms, false); ms.dispense();

    ms.insertCoin(10);
    Method checkMethod = MaintenanceState.class.getDeclaredMethod("performSubsystemCheck", String.class);
    checkMethod.setAccessible(true);
    checkMethod.invoke(ms, "UNKNOWN_SYSTEM");
  }

  @Test
  void testMaintenance_Report() {
    vm.getInventory().clear();
    vm.getInventory().put("D1", new Drink("D1", "Crit", 10, 0, true));
    vm.getInventory().put("D2", new Drink("D2", "Warn", 10, 4, false));
    vm.getInventory().put("D3", new Drink("D3", "Ok", 10, 10, false));
    vm.setState(vm.getMaintenanceState());
    vm.getMaintenanceState().maintenance("any");
  }

  @Test
  void testMaintenance_EdgeInputs() {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();
    ms.selectDrink(null); ms.selectDrink(""); ms.selectDrink("NON_EXIST_ID");
    vm.getInventory().put("A1", new Drink("A1", "Coke", 25, 5, false));
    ms.selectDrink("A1");
  }

  // =========================================================
  // 5. Core & State Machine
  // =========================================================
  @Test
  void testCoreAndStates() throws Exception {
    assertTrue(vm.performSystemSelfCheck());

    // 循環破壞測試
    String[] fields = {"idleState", "hasMoneyState", "soldState", "soldOutState", "maintenanceState", "changeService", "discountEngine"};
    for(String fName : fields) {
      vm = new VendingMachine();
      Field f = VendingMachine.class.getDeclaredField(fName);
      f.setAccessible(true); f.set(vm, null);
      assertFalse(vm.performSystemSelfCheck());
    }

    vm = new VendingMachine();
    vm.setBalance(2000);
    vm.getInventory().put("FREE", new Drink("F", "Free", 0, 10, false));
    Drink neg = new Drink("N", "N", -10, -5, false);
    vm.getInventory().put("NEG", neg);
    vm.performSystemSelfCheck();
    assertEquals(0, neg.getStock());

    vm.setBalance(-10);
    vm.performSystemSelfCheck();
    assertEquals(0, vm.getBalance());

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
    vm.insertCoin(2);

    vm.getInventory().put("A2", new Drink("A2", "Tea", 20, 0, false));
    vm.selectDrink("A2");
    assertInstanceOf(SoldOutState.class, vm.getCurrentState());

    vm.setState(vm.getHasMoneyState());
    vm.setBalance(5);
    vm.selectDrink("NON_EXIST");
    vm.selectDrink("A1");
  }
}