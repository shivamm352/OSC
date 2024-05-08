package com.springosc.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedProductDTO {

    public String productId;
    public String categoryId;
    public String prodName;
    public double prodMarketPrice;

}
