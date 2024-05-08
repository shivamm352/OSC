package com.springosc.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailsDTO {

    @JsonProperty("TYPE")
    public String type;

    @JsonProperty("Featured Products")
    public ArrayList<FeaturedProductDTO> featuredProducts;

    @JsonProperty("Categories")
    public ArrayList<CategoryDTO> categories;

}
