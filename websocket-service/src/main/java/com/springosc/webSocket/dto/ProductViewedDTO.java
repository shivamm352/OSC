package com.springosc.webSocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductViewedDTO {

    String userId;
    String productId;
    String categoryId;

    public ProductViewedDTO() {

    }
}
