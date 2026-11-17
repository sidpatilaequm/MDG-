package com.example.multimedia.file_upload_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionItemDto {
    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String permissionType; // MODULE, SUB_MODULE, BLOCK
    private Boolean view;
    private Boolean create;
    private Boolean edit;
    private Boolean delete;
    /** HIDDEN/READONLY/EDITABLE/MANDATORY - only set when permissionType is BLOCK. */
    private String fieldMode;
    private List<PermissionItemDto> children;
}
