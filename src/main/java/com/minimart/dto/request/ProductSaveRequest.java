package com.minimart.dto.request;

import com.minimart.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductSaveRequest {

    @NotBlank(message = "Ten san pham khong duoc de trong.")
    @Size(max = 200, message = "Ten san pham toi da 200 ky tu.")
    private String name;

    @Size(max = 100, message = "Barcode toi da 100 ky tu.")
    private String barcode;

    @NotNull(message = "Gia ban khong duoc de trong.")
    @DecimalMin(value = "0.01", message = "Gia ban phai lon hon 0.")
    @Digits(integer = 16, fraction = 2, message = "Gia ban khong hop le (toi da 16 chu so nguyen, 2 chu so thap phan).")
    private BigDecimal price;

    private Long categoryId;

    @Size(max = 500, message = "URL anh toi da 500 ky tu.")
    private String imageUrl;

    private ProductStatus status = ProductStatus.ACTIVE;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
}
