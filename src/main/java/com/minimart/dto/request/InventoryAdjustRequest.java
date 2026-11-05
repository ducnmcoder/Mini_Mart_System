package com.minimart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class InventoryAdjustRequest {

    @NotNull(message = "So luong ton kho khong duoc de trong.")
    @Min(value = 0, message = "So luong ton kho khong duoc am.")
    private Integer newQuantity;

    @NotNull(message = "Nguong canh bao khong duoc de trong.")
    @Min(value = 0, message = "Nguong canh bao khong duoc am.")
    private Integer lowStockThreshold;

    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
}
