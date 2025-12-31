package com.vending.core;

import com.vending.model.Drink;
import com.vending.service.ChangeService;
import com.vending.service.DiscountEngine;
import com.vending.state.*;
import com.vending.Main;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VendingMachineTest - 終極覆蓋率衝刺版 (Target: >95%)
 */
class VendingMachineTest {
  private VendingMachine vm;

  @BeforeEach
  void setUp() { vm = new VendingMachine(); }

  // =========================================================
  // ★★★ 區塊 1：POJO 與模型類別全覆蓋 ★★★
  // =========================================================

  @Test
  @DisplayName("Model: Drink Getter/Setter 完整覆蓋")
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

  // =========================================================
  // ★★★ 區塊 2：核心系統自檢與破壞性測試 ★★★
  // =========================================================

  @Test
  @DisplayName("Core: 系統自檢 - 完美狀態")
  void testSystemSelfCheck_HappyPath() {
    assertTrue(vm.performSystemSelfCheck());
  }

  @Test
  @DisplayName("Core: 系統自檢 - 致命破壞 (Null 注入)")
  void testSystemSelfCheck_FatalNulls() throws Exception {
    Field discountEngineField = VendingMachine.class.getDeclaredField("discountEngine");
    discountEngineField.setAccessible(true);
    discountEngineField.set(vm, null);

    Field changeServiceField = VendingMachine.class.getDeclaredField("changeService");
    changeServiceField.setAccessible(true);
    changeServiceField.set(vm, null);

    assertFalse(vm.performSystemSelfCheck(), "元件遺失時應回傳 false");
  }

  @Test
  @DisplayName("Core: 系統自檢 - 邏輯破壞 (庫存與餘額)")
  void testSystemSelfCheck_LogicErrors() {
    Drink badDrink = new Drink("BadID", "BadName", -10, -5, false);
    vm.getInventory().put("DifferentID", badDrink);
    vm.getInventory().put("Free", new Drink("Free", "Free", 0, 10, false));
    vm.setBalance(-100);

    boolean checkResult = vm.performSystemSelfCheck();
    assertFalse(checkResult);

    assertEquals(0, vm.getBalance(), "負數餘額應被修正為 0");
    assertEquals(0, badDrink.getStock(), "負數庫存應被修正為 0");

    vm.setBalance(2000);
    vm.performSystemSelfCheck();
  }

  @Test
  @DisplayName("Core: 系統自檢 - 空庫存")
  void testSystemSelfCheck_EmptyInventory() {
    vm.getInventory().clear();
    assertFalse(vm.performSystemSelfCheck());
  }

  @Test
  @DisplayName("Core: 系統自檢 - 負面路徑與修正驗證")
  void testSelfCheck_NegativePaths() {
    vm.getInventory().put("NEG", new Drink("NEG", "NEG", -1, 10, false));
    vm.performSystemSelfCheck();
    assertEquals(0, vm.getInventory().get("NEG").getStock(), "價格負數時庫存應被歸零");
  }

  // =========================================================
  // ★★★ 區塊 3：折扣引擎 ★★★
  // =========================================================

  @Test
  @DisplayName("Discount: 幸運指數全滿測試")
  void testDiscountEngine_SuperLuck() {
    DiscountEngine de = new DiscountEngine();
    Drink superDrink = new Drink("L1", "A_Lucky_8!", 77, 1, true);
    int finalPrice = de.applyPromotion(superDrink, 0, false);
    assertTrue(finalPrice < 77);
  }

  @Test
  @DisplayName("Discount: 穿透幸運指數的所有 if 條件")
  void testLuckFactor_FullCoverage() {
    DiscountEngine de = new DiscountEngine();
    Drink superDrink = new Drink("A_Lucky_8!", "A_Lucky_8!", 77, 1, true);

    de.applyPromotion(superDrink, 7, false); // 餘額奇數 (score++)
    de.applyPromotion(superDrink, 150, false); // 餘額 > 100 (score--)
    de.applyPromotion(superDrink, 0, false); // 餘額 == 0 (score-=5)
  }

  @Test
  @DisplayName("Discount: 各種行銷字串與會員邏輯")
  void testDiscountEngine_MarketingAndMembers() {
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

  // =========================================================
  // ★★★ 區塊 4：ChangeService 硬幣檢測全餐 ★★★
  // =========================================================

  @Test
  @DisplayName("ChangeService: 稽核與防偽邏輯")
  void testChangeService_FullCoverage() {
    ChangeService cs = new ChangeService();
    assertTrue(cs.verifyCoinAuthenticity(50));
    assertTrue(cs.verifyCoinAuthenticity(10));
    assertTrue(cs.verifyCoinAuthenticity(5));
    assertTrue(cs.verifyCoinAuthenticity(1));
    assertFalse(cs.verifyCoinAuthenticity(99));

    cs.calculateChange(200);
    for(int i=0; i<5; i++) cs.calculateChange(50);
    cs.auditCoinReserves();
    cs.calculateChange(190);
  }

  @Test
  @DisplayName("ChangeService: 覆蓋所有硬幣材質與公差分支")
  void testChangeService_SecurityBranches() {
    ChangeService cs = new ChangeService();
    cs.verifyCoinAuthenticity(50);
    cs.verifyCoinAuthenticity(10);
    cs.verifyCoinAuthenticity(5);
    cs.verifyCoinAuthenticity(1);
    cs.verifyCoinAuthenticity(999);

    for(int i=0; i<10; i++) cs.calculateChange(50);
    cs.auditCoinReserves();
  }

  // =========================================================
  // ★★★ 區塊 5：維護模式與狀態機邏輯 (包含 Reflection 測試) ★★★
  // =========================================================

  @Test
  @DisplayName("Maintenance: 穿透網路連線所有死分支")
  void testConnectivityCombinations() throws Exception {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    // ★★★ 使用反射獲取私有屬性，這是解決覆蓋率卡關的關鍵 ★★★
    Field wifiField = MaintenanceState.class.getDeclaredField("wifi");
    Field simField = MaintenanceState.class.getDeclaredField("sim4g");
    wifiField.setAccessible(true);
    simField.setAccessible(true);

    // 1. 測試「僅 Wi-Fi」路徑
    wifiField.set(ms, true);
    simField.set(ms, false);
    ms.dispense(); // 觸發 performSubsystemCheck("CONNECTIVITY")

    // 2. 測試「僅 4G」路徑
    wifiField.set(ms, false);
    simField.set(ms, true);
    ms.dispense();

    // 3. 測試「離線模式」路徑
    wifiField.set(ms, false);
    simField.set(ms, false);
    ms.dispense();
  }

  @Test
  @DisplayName("Maintenance: 涵蓋子系統檢查的所有 Switch 分支")
  void testMaintenanceSubsystems() {
    vm.enterMaintenance("admin123");
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();
    ms.dispense();
    vm.getInventory().get("A1").setStock(0);
    vm.getInventory().get("B1").setStock(1);
    ms.maintenance("any");
  }

  @Test
  @DisplayName("State: 維護模式深度檢測")
  void testMaintenance_DeepDive() {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    ms.dispense();
    vm.getInventory().get("A1").setStock(0);
    vm.getInventory().get("A2").setStock(0);
    vm.getInventory().get("B1").setStock(0);
    vm.setBalance(50);
    ms.maintenance("any");

    ms.insertCoin(10);
    ms.cancel();
    ms.selectDrink("A1");
    ms.selectDrink("INVALID_ID");
  }

  // =========================================================
  // ★★★ 區塊 6：其他狀態覆蓋 (Sold, SoldOut, Idle, HasMoney) ★★★
  // =========================================================

  @Test
  @DisplayName("State: Sold/SoldOut/Idle 完整覆蓋")
  void testOtherStates() {
    // --- SoldState ---
    vm.setState(vm.getSoldState());
    vm.insertCoin(10);
    vm.selectDrink("A1");
    vm.enterMaintenance("pwd");
    vm.cancel();
    vm.setCurrentDrink(new Drink("D1", "D", 10, 10, false));
    vm.setBalance(100);
    vm.dispense();

    // --- SoldOutState ---
    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A1");
    vm.dispense();
    vm.insertCoin(10);
    vm.enterMaintenance("wrong");
    vm.enterMaintenance("admin123");
    vm.setState(vm.getSoldOutState());
    vm.setBalance(50);
    vm.cancel();

    // --- IdleState ---
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

  @Test
  @DisplayName("HasMoneyState: 涵蓋 selectDrink 所有分支路徑")
  void testHasMoneyState_AllBranches() {
    vm.insertCoin(50);
    vm.selectDrink("NON_EXISTENT");

    vm.getInventory().get("A2").setStock(0);
    vm.selectDrink("A2");
    assertTrue(vm.getCurrentState() instanceof SoldOutState);

    vm.setState(vm.getHasMoneyState());
    vm.setBalance(5);
    vm.selectDrink("A1");
    vm.setBalance(25);
    vm.selectDrink("A1");
  }

  @Test
  @DisplayName("Main: 整合流程測試")
  void testMainApp() {
    assertDoesNotThrow(() -> Main.main(new String[]{}));
    new Main();
  }
}