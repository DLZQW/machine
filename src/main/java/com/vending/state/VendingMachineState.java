// File: com/vending/state/VendingMachineState.java
package com.vending.state;

public interface VendingMachineState {
  void insertCoin(int amount);
  void selectDrink(String drinkId);
  void dispense();
  void cancel();
  void maintenance(String password);
}