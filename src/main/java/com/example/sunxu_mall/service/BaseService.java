package com.example.sunxu_mall.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.sunxu_mall.dto.BasePageQuery;
import com.example.sunxu_mall.dto.file.FileDTO;
import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.model.ResponsePageEntity;
import com.example.sunxu_mall.util.FileUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/10 9:35
 * @description 基础服务类，提供通用的分页查询和Excel导出功能 (适配 PageHelper)
 */
@Slf4j
public abstract class BaseService<K, V extends BasePageQuery> {

    private static final String FILE_TYPE_EXCEL = "application/vnd.ms-excel";
    private static final String FILE_BIZ_TYPE = "file";

    @Value("${mall.mgt.exportPageSize:2}")
    private int exportPageSize;

    @Value("${mall.mgt.sheetDataSize:4}")
    private int sheetDataSize;

    @Value("${mall.mgt.temp-path:./temp/}")
    private String tempFilePath;

    @Autowired
    private UploadService uploadService;

    /**
     * 子类需实现此方法，根据查询条件返回列表数据
     *
     * @param query 查询条件
     * @return 数据列表
     */
    protected abstract List<K> selectList(V query);

    /**
     * 通用的分页接口
     *
     * @param query 查询条件
     * @return 分页数据
     */
    public ResponsePageEntity<K> searchByPage(V query) {
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();

        // 默认分页参数
        if (pageNum == null) pageNum = 1;
        if (pageSize == null) pageSize = 10;

        PageHelper.startPage(pageNum, pageSize);
        List<K> dataList = selectList(query);
        PageInfo<K> pageInfo = new PageInfo<>(dataList);

        return new ResponsePageEntity<>((long) pageNum, (long) pageSize, pageInfo.getTotal(), dataList);
    }

    /**
     * 公共excel导出方法
     *
     * @param query     查询条件
     * @param fileName  文件名称
     * @param clazzName 实体类名称
     * @return 下载地址
     */
    public String export(V query, String fileName, String clazzName) {
        File file = prepareExportFile(fileName);

        // 获取总数
        long totalCount = getTotalCount(query);

        // 获取导出实体类
        Class<?> clazz = getExportClass(clazzName);

        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(file).build();
            
            // 执行核心写入逻辑
            fillExcelData(excelWriter, query, totalCount, clazz);

        } catch (Exception e) {
            log.warn("Export Excel file exception", e);
            throw new BusinessException("导出Excel文件异常: " + e.getMessage());
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }

        try {
            return uploadFileToOss(fileName, file, file.getAbsolutePath());
        } finally {
            // 删除临时文件，防止磁盘写满
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    private File prepareExportFile(String fileName) {
        String downloadPath = tempFilePath + fileName + ".xlsx";
        File file = new File(downloadPath);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        return file;
    }

    private long getTotalCount(V query) {
        query.setPageNum(1);
        query.setPageSize(1);
        PageHelper.startPage(1, 1, true);
        List<K> countList = selectList(query);
        PageInfo<K> countPageInfo = new PageInfo<>(countList);
        return countPageInfo.getTotal();
    }

    private Class<?> getExportClass(String clazzName) {
        if (clazzName == null) {
            return null;
        }
        try {
            return Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            log.warn("Data export exception, class not found: {}", clazzName);
            throw new BusinessException(String.format("数据导出异常，没有找到类:%s", clazzName));
        }
    }

    private void fillExcelData(ExcelWriter excelWriter, V query, long totalCount, Class<?> clazz) {
        // 计算 Sheet 数量
        int sheetCount = (int) ((totalCount + sheetDataSize - 1) / sheetDataSize);
        if (sheetCount == 0) {
            sheetCount = 1;
        }
        // 每个 Sheet 需要循环查询数据库的次数
        int loopCount = (sheetDataSize + exportPageSize - 1) / exportPageSize;

        // 恢复分页大小
        query.setPageSize(exportPageSize);

        // 全局页码计数器
        int globalPageNo = 1;

        for (int sheetIndex = 1; sheetIndex <= sheetCount; sheetIndex++) {
            WriteSheet writeSheet = buildWriteSheet(sheetIndex, clazz);
            
            int currentLoop = 0;
            while (currentLoop < loopCount) {
                query.setPageNum(globalPageNo);
                PageHelper.startPage(globalPageNo, exportPageSize, false);
                List<K> dataEntities = selectList(query);
                
                if (CollectionUtil.isEmpty(dataEntities)) {
                    if (sheetIndex == 1 && currentLoop == 0) {
                            excelWriter.write(Collections.emptyList(), writeSheet);
                    }
                    break;
                }

                excelWriter.write(dataEntities, writeSheet);

                globalPageNo++;
                currentLoop++;
                
                if (dataEntities.size() < exportPageSize) {
                    break;
                }
            }
        }
    }

    private WriteSheet buildWriteSheet(int sheetIndex, Class<?> clazz) {
        String sheetName = "Sheet" + sheetIndex;
        if (clazz != null) {
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
