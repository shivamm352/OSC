package com.springosc.product.dto;

import com.springosc.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {


    private String categoryId;
    private String productId;
    private String productName;
    private double prodMarketPrice;
    private String productDescription;
    private int viewCount;

    public ProductDTO(String categoryId, String productId, Product product) {
        this.categoryId = categoryId;
        this.productId = productId;
        this.productName = product.getProductName();
        this.prodMarketPrice = product.getProductPrice();
        this.productDescription = product.getProductDescription();
        this.viewCount = product.getViewCount();
    }

    public ProductDTO(String productId, String categoryId, String productName, double prodMarketPrice) {
    }
}
