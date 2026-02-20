package com.cda.carpooling.mapper;

import com.cda.carpooling.dto.response.BrandResponse;
import com.cda.carpooling.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {
    public BrandResponse toResponse(Brand brand) {
        if (brand == null) {
            return null;
        }

        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .build();
    }
}
