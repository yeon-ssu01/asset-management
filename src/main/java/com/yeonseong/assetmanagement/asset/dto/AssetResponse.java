package com.yeonseong.assetmanagement.asset.dto;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import com.yeonseong.assetmanagement.asset.domain.AssetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        String assetTag,
        String serialNo,
        AssetCategory category,
        String manufacturer,
        String modelName,
        LocalDate purchaseDate,
        BigDecimal purchasePrice,
        LocalDate warrantyEndDate,
        AssetStatus status,
        String location,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getAssetTag(),
                asset.getSerialNo(),
                asset.getCategory(),
                asset.getManufacturer(),
                asset.getModelName(),
                asset.getPurchaseDate(),
                asset.getPurchasePrice(),
                asset.getWarrantyEndDate(),
                asset.getStatus(),
                asset.getLocation(),
                asset.getMemo(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
