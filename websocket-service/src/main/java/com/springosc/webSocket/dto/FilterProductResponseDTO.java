package com.springosc.webSocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterProductResponseDTO {

    private String productId;
    private String catId;
    private String prodName;
    private String prodMarketPrice;

}
