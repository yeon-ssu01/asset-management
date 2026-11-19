package com.yeonseong.assetmanagement.asset.repository;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    boolean existsByAssetTag(String assetTag);

    boolean existsBySerialNo(String serialNo);

    boolean existsByAssetTagAndIdNot(String assetTag, Long id);

    boolean existsBySerialNoAndIdNot(String serialNo, Long id);
}
