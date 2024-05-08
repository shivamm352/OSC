package com.springosc.webSocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimilarProductResponseDTO {

    private String categoryId;
    private String productId;
    private String prodName;
    private double prodMarketPrice;

    public SimilarProductResponseDTO() {

    }
}
