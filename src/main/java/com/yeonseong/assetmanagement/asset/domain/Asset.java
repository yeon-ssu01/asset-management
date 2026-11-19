package com.yeonseong.assetmanagement.asset.domain;

import com.yeonseong.assetmanagement.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String assetTag;

    @Column(unique = true, length = 100)
    private String serialNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetCategory category;

    @Column(length = 50)
    private String manufacturer;

    @Column(nullable = false, length = 100)
    private String modelName;

    private LocalDate purchaseDate;

    @Column(precision = 12, scale = 0)
    private BigDecimal purchasePrice;

    private LocalDate warrantyEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @Column(length = 100)
    private String location;

    @Column(length = 500)
    private String memo;

    @Builder
    private Asset(String assetTag, String serialNo, AssetCategory category,
                  String manufacturer, String modelName, LocalDate purchaseDate,
                  BigDecimal purchasePrice, LocalDate warrantyEndDate,
                  String location, String memo) {
        this.assetTag = assetTag;
        this.serialNo = serialNo;
        this.category = category;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.warrantyEndDate = warrantyEndDate;
        this.location = location;
        this.memo = memo;
        this.status = AssetStatus.IN_STOCK;
    }

    public void update(String assetTag, String serialNo, AssetCategory category,
                       String manufacturer, String modelName, LocalDate purchaseDate,
                       BigDecimal purchasePrice, LocalDate warrantyEndDate,
                       String location, String memo) {
        this.assetTag = assetTag;
        this.serialNo = serialNo;
        this.category = category;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.warrantyEndDate = warrantyEndDate;
        this.location = location;
        this.memo = memo;
    }

    public void changeStatus(AssetStatus status) {
        this.status = status;
    }
}
