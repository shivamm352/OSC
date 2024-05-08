package com.springosc.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductsDTO {

    private String categoryId;
    private String productId;
    private String productName;
    private double prodMarketPrice;
    private String productDescription;
    private int viewCount;

}
