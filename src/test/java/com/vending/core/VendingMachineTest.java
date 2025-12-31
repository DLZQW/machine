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
 * VendingMachineTest - 修正 Failures 版
 */
class VendingMachineTest {
  private VendingMachine vm;

  @BeforeEach
  void setUp() { vm = new VendingMachine(); }

  // -----------------------------------------------------------
  // 1. 模型與基本測試
  // -----------------------------------------------------------
  @Test
  @DisplayName("Model: Drink Getter/Setter")
  void testDrinkPojo() {
    Drink d = new Drink("T1", "Test", 100, 10, true);
    assertEquals("T1", d.getId());
    assertEquals("Test", d.getName());
    assertEquals(100, d.getPrice());
    assertEquals(10, d.getStock());
    assertTrue(d.isHot());
    d.setStock(50);
    assertEquals(50, d.getStock());
    new Drink(null, null, 0, 0, false);
  }

  @Test
  @DisplayName("Main: 整合流程與私有建構子覆蓋")
  void testMainApp() {
    assertDoesNotThrow(() -> Main.main(new String[]{}));
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

  // -----------------------------------------------------------
  // 2. 系統自檢與破壞測試
  // -----------------------------------------------------------
  @Test
  @DisplayName("Core: 系統自檢 - 致命破壞 (Null 注入)")
  void testSystemSelfCheck_FatalNulls() throws Exception {
    Field discountEngineField = VendingMachine.class.getDeclaredField("discountEngine");
    discountEngineField.setAccessible(true);
    discountEngineField.set(vm, null);

    Field changeServiceField = VendingMachine.class.getDeclaredField("changeService");
    changeServiceField.setAccessible(true);
    changeServiceField.set(vm, null);

    assertFalse(vm.performSystemSelfCheck());
  }

  @Test
  @DisplayName("Core: 系統自檢 - 邏輯異常")
  void testSystemSelfCheck_LogicErrors() {
    Drink badDrink = new Drink("BadID", "BadName", -10, -5, false);
    vm.getInventory().put("DifferentID", badDrink);
    vm.getInventory().put("Free", new Drink("Free", "Free", 0, 10, false));
    vm.setBalance(-100);

    boolean checkResult = vm.performSystemSelfCheck();
    assertFalse(checkResult);
    assertEquals(0, vm.getBalance());
    assertEquals(0, badDrink.getStock());

    vm.setBalance(2000);
    vm.performSystemSelfCheck();

    vm.getInventory().clear();
    assertFalse(vm.performSystemSelfCheck());
  }

  // -----------------------------------------------------------
  // 3. 折扣引擎測試
  // -----------------------------------------------------------
  @Test
  @DisplayName("DiscountEngine: 精確邊界值測試")
  void testDiscountBoundaries() {
    DiscountEngine de = new DiscountEngine();
    // 測試 balance > 100 的邊界 (現在 applyPromotion 裡有這條規則了)
    Drink cheap = new Drink("D1", "Cheap", 30, 10, false);
    assertEquals(30, de.applyPromotion(cheap, 100, false)); // 剛好 100 -> 原價
    assertEquals(25, de.applyPromotion(cheap, 101, false)); // 101 -> 折 5 元

    Drink d40 = new Drink("D40", "Price40", 40, 10, false);
    assertEquals(40, de.applyPromotion(d40, 50, false)); // Price 40 -> 原價
  }

  @Test
  @DisplayName("Discount: 幸運指數全覆蓋")
  void testDiscountEngine_LuckLogic() {
    DiscountEngine de = new DiscountEngine();
    Drink superDrink = new Drink("L1", "A_Lucky_8!", 77, 1, true);
    int p1 = de.applyPromotion(superDrink, 0, false);
    assertTrue(p1 < 77);

    Drink boringDrink = new Drink("B1", "B", 13, 5, false);
    de.applyPromotion(boringDrink, 50, false);
  }

  @Test
  @DisplayName("Discount: 行銷字串與各種分支")
  void testDiscountMarketing() {
    DiscountEngine de = new DiscountEngine();
    Drink expensive = new Drink("E1", "Luxury Coffee", 50, 10, true);
    de.generateMarketingMessage(expensive);
    Drink cheap = new Drink("C1", "Cheap Tea", 10, 10, false);
    de.generateMarketingMessage(cheap);
    Drink soda = new Drink("S1", "Coke Soda", 25, 10, false);
    de.generateMarketingMessage(soda);
    Drink water = new Drink("W1", "Mineral Water", 20, 10, false);
    de.generateMarketingMessage(water);

    de.applyPromotion(soda, 0, false);
    de.applyPromotion(cheap, 100, false);
    de.applyPromotion(expensive, 0, true);

    soda.setStock(20);
    de.applyPromotion(soda, 0, false);
    soda.setStock(2);
    de.applyPromotion(soda, 0, false);
  }

  // -----------------------------------------------------------
  // 4. ChangeService (Reflection 介入)
  // -----------------------------------------------------------
  @Test
  @DisplayName("ChangeService: 偽幣與異常重量測試")
  void testChangeService_Counterfeit() throws Exception {
    ChangeService cs = new ChangeService();
    cs.calculateChange(66);

    Field w50 = ChangeService.class.getDeclaredField("weight50");
    w50.setAccessible(true);
    w50.setDouble(cs, 100.0);

    Map<Integer, Integer> result = cs.calculateChange(50);
    assertEquals(5, result.get(10));
    assertNull(result.get(50));
  }

  @Test
  @DisplayName("ChangeService: 庫存狀態全分支")
  void testChangeService_InventoryStatus() {
    ChangeService cs = new ChangeService();
    cs.auditCoinReserves();
    for(int i=0; i<15; i++) cs.calculateChange(10);
    cs.auditCoinReserves();
    cs.calculateChange(250);
    cs.auditCoinReserves();
  }

  @Test
  @DisplayName("ChangeService: 強制清空特定硬幣 (Reflection)")
  void testChangeServiceDrainSpecificCoins() throws Exception {
    ChangeService cs = new ChangeService();

    // ★★★ 修正重點：使用 Reflection 強制將 10 元硬幣歸零 ★★★
    // 因為一般的 calculateChange 會有「安全水位」保護，無法真正清空
    Field storageField = ChangeService.class.getDeclaredField("coinStorage");
    storageField.setAccessible(true);
    Map<Integer, Integer> storage = (Map<Integer, Integer>) storageField.get(cs);

    // 強制設定 10 元為 0 個
    storage.put(10, 0);

    // 要求找 20 元 (正常會給 2 個 10 元，現在應該被迫給 4 個 5 元)
    Map<Integer, Integer> result = cs.calculateChange(20);

    // 驗證確實沒有 10 元
    assertTrue(!result.containsKey(10) || result.get(10) == 0);
    // 驗證總金額正確
    assertEquals(20, result.entrySet().stream().mapToInt(e->e.getKey()*e.getValue()).sum());
  }

  @Test
  @DisplayName("ChangeService: 負數金額防呆")
  void testChangeServiceEdges() {
    ChangeService cs = new ChangeService();
    Map<Integer, Integer> result = cs.calculateChange(-10);
    assertTrue(result == null || result.isEmpty());
    result = cs.calculateChange(0);
    assertTrue(result == null || result.isEmpty());
  }

  // -----------------------------------------------------------
  // 5. 維護模式 (Reflection 介入)
  // -----------------------------------------------------------
  @Test
  @DisplayName("Maintenance: 感應器異常分支覆蓋")
  void testMaintenance_Sensors() throws Exception {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    Field tempField = MaintenanceState.class.getDeclaredField("currentTemp");
    tempField.setAccessible(true);
    Field voltField = MaintenanceState.class.getDeclaredField("currentVoltage");
    voltField.setAccessible(true);
    Field wifiField = MaintenanceState.class.getDeclaredField("wifi");
    wifiField.setAccessible(true);
    Field simField = MaintenanceState.class.getDeclaredField("sim4g");
    simField.setAccessible(true);

    tempField.setInt(ms, 50);
    ms.dispense();
    tempField.setInt(ms, -5);
    ms.dispense();
    voltField.setInt(ms, 220);
    ms.dispense();
    wifiField.set(ms, true); simField.set(ms, false);
    ms.dispense();
    wifiField.set(ms, false); simField.set(ms, true);
    ms.dispense();
    wifiField.set(ms, false); simField.set(ms, false);
    ms.dispense();
  }

  @Test
  @DisplayName("MaintenanceState: 庫存報告多重條件覆蓋")
  void testInventoryReportBranches() {
    vm.setState(vm.getMaintenanceState());
    vm.getInventory().clear();
    vm.getInventory().put("D1", new Drink("D1", "缺貨品", 30, 0, false));
    vm.getInventory().put("D2", new Drink("D2", "低庫存貴", 35, 2, false));
    vm.getInventory().put("D3", new Drink("D3", "低庫存俗", 10, 2, false));
    vm.getInventory().put("D4", new Drink("D4", "庫存足", 20, 10, false));
    vm.enterMaintenance("admin123");
  }

  @Test
  @DisplayName("MaintenanceState: 覆蓋無效商品ID分支")
  void testMaintenanceInvalidSelect() {
    vm.setState(vm.getMaintenanceState());
    vm.selectDrink("NON_EXIST_ID");
    vm.selectDrink("");
    vm.selectDrink(null);
  }

  // -----------------------------------------------------------
  // 6. 狀態機與其他
  // -----------------------------------------------------------
  @Test
  @DisplayName("HasMoneyState: 已投幣狀態下的無效硬幣測試")
  void testHasMoneyInvalidCoin() {
    vm.insertCoin(10);
    vm.insertCoin(3);
    assertEquals(10, vm.getBalance());
    assertInstanceOf(HasMoneyState.class, vm.getCurrentState());
    vm.insertCoin(-5);
    assertEquals(10, vm.getBalance());
  }

  @Test
  @DisplayName("HasMoneyState: 涵蓋 selectDrink 所有分支路徑")
  void testHasMoneyState_AllBranches() {
    vm.insertCoin(50);
    vm.selectDrink("NON_EXISTENT");
    vm.getInventory().get("A2").setStock(0);
    vm.selectDrink("A2");
    assertInstanceOf(SoldOutState.class, vm.getCurrentState());
    vm.setState(vm.getHasMoneyState());
    vm.setBalance(5);
    vm.selectDrink("A1");
    vm.setBalance(25);
    vm.selectDrink("A1");
  }

  @Test
  @DisplayName("State: 其他狀態覆蓋")
  void testOtherStates() {
    vm.setState(vm.getSoldState());
    vm.insertCoin(10);
    vm.selectDrink("A1");
    vm.enterMaintenance("pwd");
    vm.cancel();
    vm.setCurrentDrink(new Drink("D1", "D", 10, 10, false));
    vm.setBalance(100);
    vm.dispense();

    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A1");
    vm.dispense();
    vm.insertCoin(10);
    vm.enterMaintenance("wrong");
    vm.enterMaintenance("admin123");
    vm.setState(vm.getSoldOutState());
    vm.setBalance(50);
    vm.cancel();

    vm.setState(vm.getIdleState());
    vm.selectDrink("A1");
    vm.dispense();
    vm.cancel();
    vm.insertCoin(10);
    vm.setState(vm.getIdleState());
    vm.insertCoin(3);
    vm.enterMaintenance("wrong");
    vm.enterMaintenance("admin123");
  }
}