package com.yeonseong.assetmanagement.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "이미 등록된 값과 중복되었거나 연관된 데이터가 있어 처리할 수 없습니다."),

    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "자산을 찾을 수 없습니다."),
    DUPLICATE_ASSET_TAG(HttpStatus.CONFLICT, "이미 등록된 자산번호입니다."),
    DUPLICATE_SERIAL_NO(HttpStatus.CONFLICT, "이미 등록된 시리얼 번호입니다."),
    ASSET_HAS_HISTORY(HttpStatus.CONFLICT, "이력이 있는 자산은 삭제할 수 없습니다. 폐기 처리(DISPOSED)를 사용하세요."),
    ASSET_STATUS_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT, "해당 상태로는 직접 변경할 수 없습니다. IN_USE는 지급을 통해서만 설정됩니다.");

    private final HttpStatus status;
    private final String message;
}
