package com.yeonseong.assetmanagement.asset.dto;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetCreateRequest(
        @NotBlank(message = "자산번호는 필수입니다.")
        @Size(max = 30, message = "자산번호는 30자 이하입니다.")
        String assetTag,

        @Size(max = 100, message = "시리얼 번호는 100자 이하입니다.")
        String serialNo,

        @NotNull(message = "분류는 필수입니다.")
        AssetCategory category,

        @Size(max = 50, message = "제조사는 50자 이하입니다.")
        String manufacturer,

        @NotBlank(message = "모델명은 필수입니다.")
        @Size(max = 100, message = "모델명은 100자 이하입니다.")
        String modelName,

        LocalDate purchaseDate,

        @PositiveOrZero(message = "구매가는 0 이상입니다.")
        BigDecimal purchasePrice,

        LocalDate warrantyEndDate,

        @Size(max = 100, message = "위치는 100자 이하입니다.")
        String location,

        @Size(max = 500, message = "메모는 500자 이하입니다.")
        String memo
) {
    public Asset toEntity() {
        return Asset.builder()
                .assetTag(assetTag.trim())
                .serialNo(blankToNull(serialNo))
                .category(category)
                .manufacturer(manufacturer)
                .modelName(modelName)
                .purchaseDate(purchaseDate)
                .purchasePrice(purchasePrice)
                .warrantyEndDate(warrantyEndDate)
                .location(location)
                .memo(memo)
                .build();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
