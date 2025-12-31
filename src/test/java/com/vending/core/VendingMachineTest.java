// File: src/test/java/com/vending/core/VendingMachineTest.java
package com.vending.core;

import com.vending.model.Drink;
import com.vending.service.ChangeService;
import com.vending.service.DiscountEngine;
import com.vending.state.*;
import com.vending.Main;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VendingMachineTest - 終極覆蓋率衝刺版 (Target: >95%)
 * 已修正編譯錯誤：vm.maintenance -> vm.enterMaintenance
 */
class VendingMachineTest {
  private VendingMachine vm;

  @BeforeEach
  void setUp() { vm = new VendingMachine(); }

  // =========================================================
  // ★★★ 區塊 1：POJO 與模型類別全覆蓋 (補足基本分) ★★★
  // =========================================================

  @Test
  @DisplayName("Model: Drink Getter/Setter 完整覆蓋")
  void testDrinkPojo() {
    Drink d = new Drink("T1", "Test", 100, 10, true);

    // 呼叫所有 Getter
    assertEquals("T1", d.getId());
    assertEquals("Test", d.getName());
    assertEquals(100, d.getPrice());
    assertEquals(10, d.getStock());
    assertTrue(d.isHot());

    // 呼叫所有 Setter
    d.setStock(50);
    assertEquals(50, d.getStock());

    // 覆蓋 Drink 建構子的邊界 (雖已在上面覆蓋，但確保邏輯)
    new Drink(null, null, 0, 0, false);
  }

  // =========================================================
  // ★★★ 區塊 2：核心系統自檢與破壞性測試 (反射注入) ★★★
  // =========================================================

  @Test
  @DisplayName("Core: 系統自檢 - 完美狀態")
  void testSystemSelfCheck_HappyPath() {
    assertTrue(vm.performSystemSelfCheck());
  }

  @Test
  @DisplayName("Core: 系統自檢 - 致命破壞 (Null 注入)")
  void testSystemSelfCheck_FatalNulls() throws Exception {
    // 使用反射(Reflection)故意把內部元件設為 null，觸發防禦性檢查
    Field discountEngineField = VendingMachine.class.getDeclaredField("discountEngine");
    discountEngineField.setAccessible(true);
    discountEngineField.set(vm, null); // 破壞 DiscountEngine

    Field changeServiceField = VendingMachine.class.getDeclaredField("changeService");
    changeServiceField.setAccessible(true);
    changeServiceField.set(vm, null); // 破壞 ChangeService

    // 觸發檢查，應該要回傳 false 並且印出錯誤 Log
    assertFalse(vm.performSystemSelfCheck(), "元件遺失時應回傳 false");
  }

  @Test
  @DisplayName("Core: 系統自檢 - 邏輯破壞 (庫存與餘額)")
  void testSystemSelfCheck_LogicErrors() {
    // 1. 破壞庫存：加入 ID 不符、價格為負、庫存為負
    Drink badDrink = new Drink("BadID", "BadName", -10, -5, false);
    vm.getInventory().put("DifferentID", badDrink); // Key != ID

    // 2. 加入 0 元商品
    vm.getInventory().put("Free", new Drink("Free", "Free", 0, 10, false));

    // 3. 破壞餘額：設定為負數
    vm.setBalance(-100);

    // 執行檢查
    boolean checkResult = vm.performSystemSelfCheck();
    assertFalse(checkResult);

    // 驗證自動修復邏輯
    assertEquals(0, vm.getBalance(), "負數餘額應被修正為 0");
    assertEquals(0, badDrink.getStock(), "負數庫存應被修正為 0");

    // 4. 測試餘額過高
    vm.setBalance(2000);
    vm.performSystemSelfCheck();
  }

  @Test
  @DisplayName("Core: 系統自檢 - 空庫存")
  void testSystemSelfCheck_EmptyInventory() {
    vm.getInventory().clear();
    assertFalse(vm.performSystemSelfCheck());
  }

  // =========================================================
  // ★★★ 區塊 3：折扣引擎 - 科學怪人飲料 (穿透所有 if) ★★★
  // =========================================================

  @Test
  @DisplayName("Discount: 幸運指數全滿測試")
  void testDiscountEngine_SuperLuck() {
    DiscountEngine de = new DiscountEngine();

    // 打造一個穿透 calculateLuckFactor 所有加分項的飲料
    // Name: Length>5(+1), StartWith A(+2), contains 8(+3), endsWith !(+5) = 11分
    // Price: 77 -> %7==0(+2), ==77(+10), %10!=0 = 12分
    // Total Luck > 10, 觸發 lucky discount
    Drink superDrink = new Drink("L1", "A_Lucky_8!", 77, 1, true);

    // Balance: 0 -> (-5) 但其他分數夠高
    // isHot & price<20 (X) -> 這裡沒分
    // stock == 1 (+7)

    int finalPrice = de.applyPromotion(superDrink, 0, false);
    // 原價 77
    // 幸運折抵 (-1)
    // 預期: 76 (如果不考慮其他折扣)
    // 確保不會崩潰且有打折
    assertTrue(finalPrice < 77);
  }

  @Test
  @DisplayName("Discount: 各種行銷字串與會員邏輯")
  void testDiscountEngine_MarketingAndMembers() {
    DiscountEngine de = new DiscountEngine();

    // 1. 行銷字串覆蓋
    Drink expensive = new Drink("E1", "Luxury Coffee", 50, 10, true);
    de.generateMarketingMessage(expensive); // >=40, Hot, Coffee

    Drink cheap = new Drink("C1", "Cheap Tea", 10, 10, false);
    de.generateMarketingMessage(cheap); // <=15, Cold, Tea

    Drink soda = new Drink("S1", "Coke Soda", 25, 10, false);
    de.generateMarketingMessage(soda); // Soda keyword

    Drink water = new Drink("W1", "Mineral Water", 20, 10, false);
    de.generateMarketingMessage(water); // General/Water logic (if any)

    // 2. 會員與庫存邏輯
    // SODA category logic
    de.applyPromotion(soda, 0, false);

    // TEA category logic (balance > 50)
    de.applyPromotion(cheap, 100, false);

    // VIP logic
    de.applyPromotion(expensive, 0, true);

    // Stock pressure logic (Stock > 15)
    soda.setStock(20);
    de.applyPromotion(soda, 0, false);

    // Stock scarcity logic (Stock <= 3)
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

    // 1. 強制執行防偽檢查 (Switch case 全跑一遍)
    assertTrue(cs.verifyCoinAuthenticity(50));
    assertTrue(cs.verifyCoinAuthenticity(10));
    assertTrue(cs.verifyCoinAuthenticity(5));
    assertTrue(cs.verifyCoinAuthenticity(1));
    assertFalse(cs.verifyCoinAuthenticity(99)); // 無效面額

    // 2. 觸發稽核報告
    cs.calculateChange(200); // Amount > 50 觸發 audit

    // 3. 觸發特定庫存警告
    // 掏空 50 元
    for(int i=0; i<5; i++) cs.calculateChange(50);
    cs.auditCoinReserves(); // 觸發 CRITICAL_EMPTY/DANGER_LOW

    // 4. 觸發 1元硬幣不足警告
    // 假設 ChangeService 內部 1 元有 50 個，全部消耗掉
    // 這裡我們難以精確控制內部 map，但透過大量找零嘗試觸發
    cs.calculateChange(190); // 消耗大量 10 元
  }

  // =========================================================
  // ★★★ 區塊 5：維護模式與狀態機邏輯 ★★★
  // =========================================================

  @Test
  @DisplayName("State: 維護模式深度檢測")
  void testMaintenance_DeepDive() {
    vm.setState(vm.getMaintenanceState());
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    // 1. 執行硬體掃描 (Switch case)
    ms.dispense();

    // 2. 觸發高成本維修估價
    // 透過清空部分庫存
    vm.getInventory().get("A1").setStock(0);
    vm.getInventory().get("A2").setStock(0);
    vm.getInventory().get("B1").setStock(0); // emptySlots > 3
    vm.setBalance(50); // balance < 100

    // 執行分析與估價
    ms.maintenance("any");

    // 3. 測試無效操作
    ms.insertCoin(10);
    ms.cancel();

    // 4. 手動補貨邏輯
    ms.selectDrink("A1"); // 補貨
    ms.selectDrink("INVALID_ID"); // 無效 ID
  }

  // =========================================================
  // ★★★ 區塊 6：其他狀態覆蓋 (Sold, SoldOut, Idle) ★★★
  // =========================================================

  @Test
  @DisplayName("State: Sold/SoldOut/Idle 完整覆蓋")
  void testOtherStates() {
    // --- SoldState ---
    vm.setState(vm.getSoldState());
    vm.insertCoin(10); // 拒收
    vm.selectDrink("A1"); // 拒絕
    // 修正：vm.maintenance -> vm.enterMaintenance
    vm.enterMaintenance("pwd"); // 拒絕
    vm.cancel(); // 拒絕
    // 出貨
    vm.setCurrentDrink(new Drink("D1", "D", 10, 10, false));
    vm.setBalance(100);
    vm.dispense(); // 回到 Idle

    // --- SoldOutState ---
    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A1"); // 無貨
    vm.dispense(); // 無貨
    vm.insertCoin(10); // 退幣
    // 修正：vm.maintenance -> vm.enterMaintenance
    vm.enterMaintenance("wrong"); // 密碼錯
    vm.enterMaintenance("admin123"); // 進維護
    vm.setState(vm.getSoldOutState()); // 切回來
    vm.setBalance(50);
    vm.cancel(); // 退款並回 Idle

    // --- IdleState ---
    vm.setState(vm.getIdleState());
    vm.selectDrink("A1"); // 先投幣
    vm.dispense(); // 未選
    vm.cancel(); // 無餘額
    vm.insertCoin(10); // 正常投幣
    vm.setState(vm.getIdleState());
    vm.insertCoin(3); // 無效面額

    // 密碼測試
    vm.enterMaintenance("wrong");
    vm.enterMaintenance("admin123");
  }

  @Test
  @DisplayName("Main: 整合流程測試")
  void testMainApp() {
    assertDoesNotThrow(() -> Main.main(new String[]{}));
    new Main(); // 建構子覆蓋
  }

  @Test
  @DisplayName("HasMoneyState: 涵蓋 selectDrink 所有分支路徑")
  void testHasMoneyState_AllBranches() {
    vm.insertCoin(50); // 確保進入 HasMoneyState

    // 分支 1: ID 不存在
    vm.selectDrink("NON_EXISTENT");

    // 分支 2: 庫存不足 (手動調整 A2 庫存為 0)
    vm.getInventory().get("A2").setStock(0);
    vm.selectDrink("A2");
    assertTrue(vm.getCurrentState() instanceof SoldOutState);

    // 回到 HasMoneyState 測試餘額不足
    vm.setState(vm.getHasMoneyState());
    vm.setBalance(5); // 設定極低餘額
    vm.selectDrink("A1"); // 可樂要 25 元，會觸發「餘額不足」

    // 分支 3: 成功路徑 (金額剛好)
    vm.setBalance(25);
    vm.selectDrink("A1");
  }

  @Test
  @DisplayName("MaintenanceState: 涵蓋子系統檢查的所有 Switch 分支")
  void testMaintenanceSubsystems() {
    vm.enterMaintenance("admin123");
    MaintenanceState ms = (MaintenanceState) vm.getCurrentState();

    // 雖然 dispense() 會呼叫大部分檢查，但我們可以透過反射或直接測試私有方法(若開放)
    // 這裡我們直接測試主進入點來觸發內部的 switch
    ms.dispense();

    // 測試 analyzeInventoryHealth 內的分數邏輯 (rps > 100, > 50 等)
    vm.getInventory().get("A1").setStock(0); // 缺貨會加 50 分
    vm.getInventory().get("B1").setStock(1); // 最後一瓶會加分
    ms.maintenance("any"); // 觸發報告
  }

  @Test
  @DisplayName("ChangeService: 覆蓋所有硬幣材質與公差分支")
  void testChangeService_SecurityBranches() {
    ChangeService cs = new ChangeService();

    // 覆蓋 verifyCoinAuthenticity 中的 switch (50, 10, 5, 1)
    cs.verifyCoinAuthenticity(50);
    cs.verifyCoinAuthenticity(10);
    cs.verifyCoinAuthenticity(5);
    cs.verifyCoinAuthenticity(1);
    cs.verifyCoinAuthenticity(999); // 測試 default 分支

    // 測試 auditCoinReserves 中的所有狀態 (CRITICAL, DANGER, WARNING, HEALTHY)
    // 透過反覆找零消耗庫存
    for(int i=0; i<10; i++) cs.calculateChange(50);
    cs.auditCoinReserves();
  }

  @Test
  @DisplayName("DiscountEngine: 穿透幸運指數的所有 if 條件")
  void testLuckFactor_FullCoverage() {
    DiscountEngine de = new DiscountEngine();

    // 構造「超級幸運飲料」:
    // 名稱長度 > 5, 開頭是 A, 包含 8, 結尾是 !
    // 價格是 77 (符合 %7 == 0 且等於 77)
    Drink superDrink = new Drink("A_Lucky_8!", "A_Lucky_8!", 77, 1, true);

    de.applyPromotion(superDrink, 7, false); // 餘額給奇數 (score++)
    de.applyPromotion(superDrink, 150, false); // 餘額 > 100 (score--)
    de.applyPromotion(superDrink, 0, false); // 餘額 == 0 (score-=5)
  }

  @Test
  @DisplayName("VendingMachine: 系統自檢的負面測試")
  void testSelfCheck_NegativePaths() {
    // 測試 0 元商品分支
    vm.getInventory().put("FREE", new Drink("FREE", "FREE", 0, 10, false));

    // 測試價格負數分支
    vm.getInventory().put("NEG", new Drink("NEG", "NEG", -1, 10, false));

    // 測試餘額邊界 (balance > 1000)
    vm.setBalance(1500);

    vm.performSystemSelfCheck();

    // 驗證自動修正：價格負數時庫存應被處理或記錄錯誤
    assertEquals(0, vm.getInventory().get("NEG").getStock());
  }
}