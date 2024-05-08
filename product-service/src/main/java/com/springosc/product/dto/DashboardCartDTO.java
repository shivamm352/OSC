package com.springosc.product.dto;

import com.springosc.product.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCartDTO {

    private String userId;
    private String productId;
    private String prodName;
    private double prodMarketPrice;
    private int cartQty;

    public DashboardCartDTO(Cart cartEntity) {
    }
}
