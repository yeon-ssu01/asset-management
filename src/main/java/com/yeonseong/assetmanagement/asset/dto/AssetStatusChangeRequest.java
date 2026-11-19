package com.yeonseong.assetmanagement.asset.dto;

import com.yeonseong.assetmanagement.asset.domain.AssetStatus;
import jakarta.validation.constraints.NotNull;

public record AssetStatusChangeRequest(
        @NotNull(message = "변경할 상태는 필수입니다.")
        AssetStatus status
) {
}
