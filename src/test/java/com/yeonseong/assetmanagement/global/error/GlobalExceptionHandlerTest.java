package com.yeonseong.assetmanagement.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DB 제약 위반은 원인과 관계없이 409 DATA_INTEGRITY_VIOLATION")
    void handleDataIntegrity() {
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "Duplicate entry 'SN-001' for key 'asset.serial_no'");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }
}
