package com.springosc.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExistingUserProductDataDTO {

    @JsonProperty("TYPE")
    public String type;

    @JsonProperty("Recently Viewed Products")
    public List<ProductDTO> recentlyViewed; // Modified to expect List<ProductDTO>

    @JsonProperty("Similar Products")
    public List<ProductDTO> similarProducts;

    @JsonProperty("Categories")
    public ArrayList<CategoryDTO> categories;

    @JsonProperty("Cart")
    public List<DashboardCartDTO> cartProducts;

}
