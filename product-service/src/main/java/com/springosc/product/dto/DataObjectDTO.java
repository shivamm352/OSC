package com.springosc.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataObjectDTO<T> {

    private List<T> data;
    public DataObjectDTO(ArrayList<ExistingUserProductDataDTO> data) {
        this.data = (List<T>) data;
    }

}
