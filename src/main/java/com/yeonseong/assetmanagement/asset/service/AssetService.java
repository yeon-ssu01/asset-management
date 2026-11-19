package com.yeonseong.assetmanagement.asset.service;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import com.yeonseong.assetmanagement.asset.domain.AssetStatus;
import com.yeonseong.assetmanagement.asset.dto.*;
import com.yeonseong.assetmanagement.asset.repository.AssetRepository;
import com.yeonseong.assetmanagement.global.error.BusinessException;
import com.yeonseong.assetmanagement.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;

    @Transactional
    public AssetResponse create(AssetCreateRequest request) {
        Asset asset = request.toEntity();

        if (assetRepository.existsByAssetTag(asset.getAssetTag())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ASSET_TAG);
        }
        if (asset.getSerialNo() != null && assetRepository.existsBySerialNo(asset.getSerialNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NO);
        }

        return AssetResponse.from(assetRepository.save(asset));
    }

    public AssetResponse get(Long id) {
        return AssetResponse.from(findById(id));
    }

    public PageResponse<AssetResponse> list(AssetStatus status, AssetCategory category,
                                            String keyword, Pageable pageable) {
        Specification<Asset> spec = buildSpec(status, category, keyword);
        Page<AssetResponse> page = assetRepository.findAll(spec, pageable)
                .map(AssetResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public AssetResponse update(Long id, AssetUpdateRequest request) {
        Asset asset = findById(id);

        String normalizedTag = request.assetTag().trim();
        String normalizedSerial = request.normalizedSerialNo();

        if (assetRepository.existsByAssetTagAndIdNot(normalizedTag, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ASSET_TAG);
        }
        if (normalizedSerial != null && assetRepository.existsBySerialNoAndIdNot(normalizedSerial, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NO);
        }

        asset.update(normalizedTag, normalizedSerial, request.category(),
                request.manufacturer(), request.modelName(), request.purchaseDate(),
                request.purchasePrice(), request.warrantyEndDate(),
                request.location(), request.memo());

        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse changeStatus(Long id, AssetStatusChangeRequest request) {
        if (request.status() == AssetStatus.IN_USE) {
            throw new BusinessException(ErrorCode.ASSET_STATUS_CHANGE_NOT_ALLOWED);
        }
        Asset asset = findById(id);
        asset.changeStatus(request.status());
        return AssetResponse.from(asset);
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = findById(id);
        // 2단계에서 이력(assignment) 존재 여부 검사를 추가한다
        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw new BusinessException(ErrorCode.ASSET_HAS_HISTORY);
        }
        assetRepository.delete(asset);
    }

    private Asset findById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }

    private Specification<Asset> buildSpec(AssetStatus status, AssetCategory category, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("assetTag"), pattern),
                        cb.like(root.get("modelName"), pattern),
                        cb.like(root.get("serialNo"), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
