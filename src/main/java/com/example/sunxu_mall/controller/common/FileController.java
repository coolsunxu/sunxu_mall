package com.example.sunxu_mall.controller.common;

import com.example.sunxu_mall.dto.file.FileDTO;
import com.example.sunxu_mall.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传", description = "通用文件上传接口")
@RestController
@RequestMapping("/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final UploadService uploadService;

    @Operation(summary = "上传文件", description = "支持单文件上传，返回文件访问URL及相关信息")
    @PostMapping("/upload")
    public ResponseEntity<FileDTO> upload(
            @Parameter(description = "文件对象", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型") @RequestParam(value = "bizType", required = false, defaultValue = "default") String bizType,
            @Parameter(description = "文件类型") @RequestParam(value = "fileType", required = false, defaultValue = "image") String fileType
    ) {
        FileDTO fileDTO = uploadService.upload(file, bizType, fileType);
        return ResponseEntity.ok(fileDTO);
    }
}
