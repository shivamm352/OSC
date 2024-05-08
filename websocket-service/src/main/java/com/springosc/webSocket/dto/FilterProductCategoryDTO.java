package com.springosc.webSocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterProductCategoryDTO {

    @JsonProperty("MT")
    private String mt;
    private String catId;
    private List<FilterProductResponseDTO> products;

}
