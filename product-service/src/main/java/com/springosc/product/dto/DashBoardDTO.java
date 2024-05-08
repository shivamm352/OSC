package com.springosc.product.dto;

import com.osc.product_cache.CategoryProductMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashBoardDTO {

    private String userId;

    private String sessionId;

}
