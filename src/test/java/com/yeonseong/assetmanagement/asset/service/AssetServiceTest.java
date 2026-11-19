package com.yeonseong.assetmanagement.asset.service;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import com.yeonseong.assetmanagement.asset.domain.AssetStatus;
import com.yeonseong.assetmanagement.asset.dto.*;
import com.yeonseong.assetmanagement.asset.repository.AssetRepository;
import com.yeonseong.assetmanagement.global.error.BusinessException;
import com.yeonseong.assetmanagement.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @InjectMocks
    private AssetService assetService;

    @Mock
    private AssetRepository assetRepository;

    private AssetCreateRequest createRequest(String assetTag, String serialNo) {
        return new AssetCreateRequest(
                assetTag, serialNo, AssetCategory.LAPTOP,
                "LG", "gram 16",
                LocalDate.of(2026, 1, 1), BigDecimal.valueOf(1_500_000),
                LocalDate.of(2028, 1, 1), "3층 창고", null
        );
    }

    @Test
    @DisplayName("자산 등록 성공")
    void create_success() {
        AssetCreateRequest request = createRequest("HW-001", "SN-001");
        given(assetRepository.existsByAssetTag("HW-001")).willReturn(false);
        given(assetRepository.existsBySerialNo("SN-001")).willReturn(false);
        given(assetRepository.save(any(Asset.class))).willAnswer(inv -> inv.getArgument(0));

        AssetResponse response = assetService.create(request);

        assertThat(response.assetTag()).isEqualTo("HW-001");
        assertThat(response.status()).isEqualTo(AssetStatus.IN_STOCK);
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    @DisplayName("자산번호 중복 시 DUPLICATE_ASSET_TAG 예외")
    void create_duplicateAssetTag() {
        given(assetRepository.existsByAssetTag("HW-001")).willReturn(true);

        assertThatThrownBy(() -> assetService.create(createRequest("HW-001", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ASSET_TAG);
    }

    @Test
    @DisplayName("시리얼 중복 시 DUPLICATE_SERIAL_NO 예외")
    void create_duplicateSerialNo() {
        given(assetRepository.existsByAssetTag("HW-001")).willReturn(false);
        given(assetRepository.existsBySerialNo("SN-001")).willReturn(true);

        assertThatThrownBy(() -> assetService.create(createRequest("HW-001", "SN-001")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_SERIAL_NO);
    }

    @Test
    @DisplayName("없는 자산 조회 시 ASSET_NOT_FOUND 예외")
    void get_notFound() {
        given(assetRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.get(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
    }

    @Test
    @DisplayName("IN_USE로 상태 직접 변경 시 ASSET_STATUS_CHANGE_NOT_ALLOWED 예외")
    void changeStatus_inUseNotAllowed() {
        AssetStatusChangeRequest request = new AssetStatusChangeRequest(AssetStatus.IN_USE);

        assertThatThrownBy(() -> assetService.changeStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ASSET_STATUS_CHANGE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("IN_STOCK이 아닌 자산 삭제 시 ASSET_HAS_HISTORY 예외")
    void delete_notInStock() {
        Asset asset = createRequest("HW-001", null).toEntity();
        asset.changeStatus(AssetStatus.REPAIR);
        given(assetRepository.findById(1L)).willReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ASSET_HAS_HISTORY);
    }
}
