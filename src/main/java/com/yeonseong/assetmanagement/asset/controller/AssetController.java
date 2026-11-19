package com.yeonseong.assetmanagement.asset.controller;

import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import com.yeonseong.assetmanagement.asset.domain.AssetStatus;
import com.yeonseong.assetmanagement.asset.dto.*;
import com.yeonseong.assetmanagement.asset.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest request) {
        AssetResponse response = assetService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/assets/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable Long id) {
        return assetService.get(id);
    }

    @GetMapping
    public PageResponse<AssetResponse> list(
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) AssetCategory category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return assetService.list(status, category, keyword, pageable);
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable Long id,
                                @Valid @RequestBody AssetUpdateRequest request) {
        return assetService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public AssetResponse changeStatus(@PathVariable Long id,
                                      @Valid @RequestBody AssetStatusChangeRequest request) {
        return assetService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
