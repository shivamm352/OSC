package com.springosc.webSocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ResponseDTO {

    @JsonProperty("MT")
    private String mt;

    String catId;
    String prodId ;
    String prodName ;
    String prodDesc;
    double prodMarketPrice ;
    private List<SimilarProductResponseDTO> similarProducts;

    public ResponseDTO() {

    }
}
