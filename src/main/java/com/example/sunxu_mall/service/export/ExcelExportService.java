package com.example.sunxu_mall.service.export;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.sunxu_mall.config.props.MallMgtExportProperties;
import com.example.sunxu_mall.dto.BasePageQuery;
import com.example.sunxu_mall.dto.file.FileDTO;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.service.UploadService;
import com.example.sunxu_mall.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Excel 导出服务（使用游标分页循环拉取，不依赖 PageHelper）
 *
 * 目标：把“导出/文件/上传”等 IO 关注点从通用分页基类中剥离出来。
 */
@Slf4j
@Service
public class ExcelExportService {

    private static final String FILE_TYPE_EXCEL = "application/vnd.ms-excel";
    private static final String FILE_BIZ_TYPE = "file";

    private final UploadService uploadService;
    private final MallMgtExportProperties exportProperties;

    public ExcelExportService(UploadService uploadService, MallMgtExportProperties exportProperties) {
        this.uploadService = uploadService;
        this.exportProperties = exportProperties;
    }

    public <K, V extends BasePageQuery> String export(
            V query,
            String fileName,
            String clazzName,
            CursorPageFetcher<K, V> fetcher,
            Function<K, Long> idExtractor
    ) {
        if (Objects.isNull(fetcher)) {
            throw new BusinessException("导出配置异常：fetcher 不能为空");
        }
        if (Objects.isNull(idExtractor)) {
            throw new BusinessException("导出配置异常：idExtractor 不能为空");
        }

        File file = prepareExportFile(fileName);

        Class<?> clazz = getExportClass(clazzName);

        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(file).build();
            fillExcelDataWithCursor(excelWriter, query, clazz, fetcher, idExtractor);
        } catch (Exception e) {
            log.warn("Export Excel file exception", e);
            throw new BusinessException("导出Excel文件异常: " + e.getMessage());
        } finally {
            if (Objects.nonNull(excelWriter)) {
                excelWriter.finish();
            }
        }

        try {
            return uploadFileToOss(fileName, file, file.getAbsolutePath());
        } finally {
            // 删除临时文件，防止磁盘写满
            if (Objects.nonNull(file) && file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    private File prepareExportFile(String fileName) {
        String downloadPath = exportProperties.getTempPath() + fileName + ".xlsx";
        File file = new File(downloadPath);
        File parentFile = file.getParentFile();
        if (Objects.nonNull(parentFile) && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        return file;
    }

    private Class<?> getExportClass(String clazzName) {
        if (Objects.isNull(clazzName)) {
            return null;
        }
        try {
            return Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            log.warn("Data export exception, class not found: {}", clazzName);
            throw new BusinessException(String.format("数据导出异常，没有找到类:%s", clazzName));
        }
    }

    private <K, V extends BasePageQuery> void fillExcelDataWithCursor(
            ExcelWriter excelWriter,
            V query,
            Class<?> clazz,
            CursorPageFetcher<K, V> fetcher,
            Function<K, Long> idExtractor
    ) {
        int sheetIndex = 1;
        int currentSheetRowCount = 0;
        Long cursorId = null;
        boolean hasMore = true;

        WriteSheet writeSheet = buildWriteSheet(sheetIndex, clazz);

        while (hasMore) {
            List<K> dataEntities = fetcher.fetch(query, cursorId, exportProperties.getExportPageSize());

            if (CollectionUtil.isEmpty(dataEntities)) {
                // 没有更多数据：确保至少写入空表头
                if (sheetIndex == 1 && currentSheetRowCount == 0) {
                    excelWriter.write(Collections.emptyList(), writeSheet);
                }
                break;
            }

            excelWriter.write(dataEntities, writeSheet);
            currentSheetRowCount += dataEntities.size();

            // 更新游标为最后一条记录的 ID
            cursorId = idExtractor.apply(dataEntities.get(dataEntities.size() - 1));

            // 如果返回数量少于请求数量，说明没有更多数据
            if (dataEntities.size() < exportProperties.getExportPageSize()) {
                hasMore = false;
            }

            // 检查是否需要切换到新的 Sheet
            if (currentSheetRowCount >= exportProperties.getSheetDataSize() && hasMore) {
                sheetIndex++;
                writeSheet = buildWriteSheet(sheetIndex, clazz);
                currentSheetRowCount = 0;
            }
        }
    }

    private WriteSheet buildWriteSheet(int sheetIndex, Class<?> clazz) {
        String sheetName = "Sheet" + sheetIndex;
        if (Objects.nonNull(clazz)) {
            return EasyExcel.writerSheet(sheetName).head(clazz).build();
        } else {
            return EasyExcel.writerSheet(sheetName).build();
        }
    }

    private String uploadFileToOss(String fileName, File file, String defaultPath) {
        try {
            MultipartFile multipartFile = FileUtil.toMultipartFile(fileName, file);
            FileDTO fileDTO = uploadService.upload(multipartFile, FILE_BIZ_TYPE, FILE_TYPE_EXCEL);
            return fileDTO.getDownloadUrl();
        } catch (Exception e) {
            log.warn("Failed to upload excel file to oss server, return local path. Reason: {}", e.getMessage());
            return defaultPath;
        }
    }
}

