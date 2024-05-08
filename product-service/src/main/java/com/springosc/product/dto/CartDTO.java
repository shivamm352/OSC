package com.springosc.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {

    private String userId;
    private String productName;
    private String productId;
    private String categoryId;
    private double productPrice;
    private int quantity;

}
