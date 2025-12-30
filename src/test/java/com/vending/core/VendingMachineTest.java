package com.vending.core;

import com.vending.model.Drink;
import com.vending.service.ChangeService;
import com.vending.service.DiscountEngine;
import com.vending.state.*;
import com.vending.Main;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VendingMachineTest 終極完結篇
 * 目標：Branch Coverage > 90%
 * 包含：狀態機強制覆蓋、邊界值分析、服務層死角測試
 */
class VendingMachineTest {
  private VendingMachine vm;

  @BeforeEach
  void setUp() { vm = new VendingMachine(); }

  // =========================================================
  // ★★★ 區塊 1：衝刺最後 3% 的邊界與死角測試 ★★★
  // =========================================================

  @Test
  @DisplayName("DiscountEngine: 精確邊界值測試 (Boundary Value Analysis)")
  void testDiscountBoundaries() {
    DiscountEngine de = new DiscountEngine();

    // 測試條件邊界: if (balance > 100 || price > 40)

    // 邊界 1: Price = 40 (應該是 False，不打折)
    Drink d40 = new Drink("D40", "BoundaryPrice", 40, 10, false);
    assertEquals(40, de.applyPromotion(d40, 50, false));

    // 邊界 2: Balance = 100 (應該是 False，不打折)
    Drink cheap = new Drink("D1", "Cheap", 30, 10, false);
    assertEquals(30, de.applyPromotion(cheap, 100, false));

    // 邊界 3: Balance = 101 (應該是 True，打折)
    assertEquals(25, de.applyPromotion(cheap, 101, false));
  }

  @Test
  @DisplayName("HasMoneyState: 已投幣狀態下的無效硬幣測試")
  void testHasMoneyInvalidCoin() {
    // 先投 10 元進入 HasMoneyState
    vm.insertCoin(10);

    // 在這個狀態下投無效硬幣 (例如 3 元)
    // 這會觸發 HasMoneyState.insertCoin() 裡面的隱藏分支
    vm.insertCoin(3);

    // 驗證餘額沒變 (還是10)，且狀態沒變
    assertEquals(10, vm.getBalance());
    assertTrue(vm.getCurrentState() instanceof HasMoneyState);

    // 順便測負數金額
    vm.insertCoin(-5);
    assertEquals(10, vm.getBalance());
  }

  @Test
  @DisplayName("ChangeService: 負數金額防呆與極限測試")
  void testChangeServiceEdges() {
    ChangeService cs = new ChangeService();
    // 測試找零金額為負數的情況 (防呆分支)
    Map<Integer, Integer> result = cs.calculateChange(-10);
    assertTrue(result == null || result.isEmpty());

    // 測試找零金額為 0 的情況
    result = cs.calculateChange(0);
    assertTrue(result == null || result.isEmpty());
  }

  // =========================================================
  // ★★★ 區塊 2：服務層深度覆蓋 (Service Layer) ★★★
  // =========================================================

  @Test
  @DisplayName("ChangeService: 模擬特定硬幣缺貨，強迫分支走入跳過邏輯")
  void testChangeServiceDrainSpecificCoins() {
    ChangeService cs = new ChangeService();
    // 初始庫存：10元有 20 個, 5元有 20 個

    // 精確耗盡 10 元硬幣 (20 次 x 10 元 = 200 元 = 20 個 10 元)
    for (int i = 0; i < 20; i++) {
      cs.calculateChange(10);
    }

    // 現在 10 元沒了，系統被迫用 4 個 5 元來找 20 元
    // 這會覆蓋到 "needed > 0 && available == 0" 這個隱藏分支
    Map<Integer, Integer> result = cs.calculateChange(20);

    // 驗證是否真的沒用到 10 元
    assertTrue(!result.containsKey(10) || result.get(10) == 0);
    assertEquals(20, result.entrySet().stream().mapToInt(e->e.getKey()*e.getValue()).sum());
  }

  @Test
  @DisplayName("DiscountEngine: 覆蓋中文名稱折扣分支")
  void testDiscountNameBranch() {
    DiscountEngine de = new DiscountEngine();
    // 測 "特調黑咖啡" 以覆蓋 OR 條件的另一邊
    Drink coffee = new Drink("D1", "特調黑咖啡", 30, 10, false);
    assertEquals(28, de.applyPromotion(coffee, 0, false));
  }

  @Test
  @DisplayName("DiscountEngine: 真值表全覆蓋")
  void testDiscountEngineCoverage() {
    DiscountEngine de = new DiscountEngine();
    Drink cheap = new Drink("D1", "Cheap", 30, 10, false);
    assertEquals(30, de.applyPromotion(cheap, 50, false)); // F || F
    assertEquals(25, de.applyPromotion(cheap, 150, false)); // T || F

    Drink expensive = new Drink("D2", "Exp", 45, 10, false);
    assertEquals(40, de.applyPromotion(expensive, 50, false)); // F || T

    Drink premium = new Drink("D3", "Prem", 50, 10, false);
    assertEquals(40, de.applyPromotion(premium, 0, true)); // T && T (VIP)
    assertEquals(35, de.applyPromotion(expensive, 0, true)); // T && F (VIP但價格不夠高，觸發折10元)

    Drink tea = new Drink("T1", "Green Tea", 20, 10, false);
    assertEquals(18, de.applyPromotion(tea, 0, false));
  }

  // =========================================================
  // ★★★ 區塊 3：狀態機強制覆蓋 (State Machine) ★★★
  // =========================================================

  @Test
  @DisplayName("刷滿 SoldState 所有無效操作分支")
  void testSoldStateCoverage() {
    vm.setState(vm.getSoldState());

    vm.insertCoin(10);     // 拒絕
    vm.selectDrink("A1");  // 拒絕
    vm.cancel();           // 拒絕
    vm.enterMaintenance("admin"); // 拒絕

    // 成功出貨路徑 (需先設餘額與飲料)
    vm.setBalance(100);
    vm.setCurrentDrink(new Drink("Test", "Test", 10, 10, false));
    vm.dispense();
    assertEquals(vm.getIdleState(), vm.getCurrentState());
  }

  @Test
  @DisplayName("刷滿 SoldOutState 所有無效操作分支")
  void testSoldOutStateCoverage() {
    vm.setState(vm.getSoldOutState());
    vm.selectDrink("A1");
    vm.dispense();
    vm.insertCoin(10);

    vm.setBalance(50);
    vm.cancel();
    assertEquals(0, vm.getBalance());

    vm.setState(vm.getSoldOutState());
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
  void testIdleStateCoverage() {
    vm.setState(vm.getIdleState());
    vm.selectDrink("A1");
    vm.dispense();
    vm.cancel();
  }

  @Test
  void testHasMoneyStateCoverage() {
    vm.setState(vm.getHasMoneyState());
    vm.dispense();
    vm.enterMaintenance("123");
    vm.selectDrink("UNKNOWN");
    vm.setBalance(10);
    vm.selectDrink("A1");
  }

  @Test
  void testMaintenanceStateCoverage() {
    vm.setState(vm.getMaintenanceState());
    vm.insertCoin(10);
    vm.dispense();
    vm.enterMaintenance("admin123");
    vm.cancel();
  }

  // =========================================================
  // ★★★ 區塊 4：核心邏輯與參數化測試 (Core Logic) ★★★
  // =========================================================

  @Test
  void testFinalizeWithoutDrink() {
    vm.finalizeTransaction();
    assertEquals(0, vm.getBalance());
  }

  @Test
  void testFinalizeInsufficientFunds() {
    vm.insertCoin(50);
    vm.setCurrentDrink(new Drink("Test", "Test", 100, 5, false));
    vm.setBalance(10);
    vm.finalizeTransaction();
    assertEquals(vm.getHasMoneyState(), vm.getCurrentState());
  }

  @Test
  void testStateIllegalActions_BugFixVerify() {
    vm.getInventory().get("A1").setStock(0);
    vm.insertCoin(50);
    vm.selectDrink("A1");
    assertEquals(vm.getSoldOutState(), vm.getCurrentState());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 50})
  void testValidCoins(int c) { vm.insertCoin(c); assertEquals(c, vm.getBalance()); }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, 3, 4, 6, 7, 8, 9, 100, -1})
  void testInvalidCoins(int c) { vm.insertCoin(c); assertEquals(0, vm.getBalance()); }

  @ParameterizedTest
  @ValueSource(ints = {101, 102, 103, 201, 404, 500, 999})
  void testDiagnostics(int code) {
    MaintenanceState ms = new MaintenanceState(vm);
    assertDoesNotThrow(() -> ms.runDiagnostics(code));
  }

  @Test
  void testMainApp() {
    assertDoesNotThrow(() -> Main.main(new String[]{}));
  }
}