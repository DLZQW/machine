// File: src/main/java/com/vending/model/Drink.java
package com.vending.model;

public class Drink {
  private String id;
  private String name;
  private int price;
  private int stock;
  private boolean isHot;

  public Drink(String id, String name, int price, int stock, boolean isHot) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.stock = stock;
    this.isHot = isHot;
  }

  // Getters and Setters
  public String getId() { return id; } // 建議順便補上 ID 的 getter
  public String getName() { return name; }
  public int getPrice() { return price; }
  public int getStock() { return stock; }
  public void setStock(int stock) { this.stock = stock; }

  // ★★★ 補上這個方法來修復錯誤 ★★★
  public boolean isHot() { return isHot; }
}