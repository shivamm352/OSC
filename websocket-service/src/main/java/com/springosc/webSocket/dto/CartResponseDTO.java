package com.springosc.webSocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartResponseDTO {

    @JsonProperty("MT")
    private String mt;

    private String prodId;

    private String prodName;

    private double price;

    private int cartQty;

    public CartResponseDTO() {

    }
}
