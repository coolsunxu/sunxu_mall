package com.example.sunxu_mall.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.sunxu_mall.dto.FileDTO;
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/10 9:35
 * @description 基础服务类，提供通用的分页查询和Excel导出功能 (适配 PageHelper)
 */
@Slf4j
public abstract class BaseService<K, V> {

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
        Integer pageNum = getPageNum(query);
        Integer pageSize = getPageSize(query);

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
        // 1. 准备文件路径和目录
        String downloadPath = tempFilePath + fileName + ".xlsx";
        File file = new File(downloadPath);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }

        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(file).build();
            
            // 获取总数以计算 Sheet 数量
            setPageNum(query, 1);
            setPageSize(query, 1);
            
            // 只查询总数
            PageHelper.startPage(1, 1, true);
            List<K> countList = selectList(query);
            PageInfo<K> countPageInfo = new PageInfo<>(countList);
            long totalCount = countPageInfo.getTotal();

            // 计算 Sheet 数量
            int sheetCount = (int) ((totalCount + sheetDataSize - 1) / sheetDataSize);
            if (sheetCount == 0) {
                sheetCount = 1;
            }
            // 每个 Sheet 需要循环查询数据库的次数
            int loopCount = (sheetDataSize + exportPageSize - 1) / exportPageSize;

            Class<?> clazz = null;
            try {
                if (clazzName != null) {
                    clazz = Class.forName(clazzName);
                }
            } catch (ClassNotFoundException e) {
                log.warn("数据导出异常，没有找到类:{}", clazzName);
                throw new BusinessException(String.format("数据导出异常，没有找到类:%s", clazzName));
            }
            
            // 恢复分页大小
            setPageSize(query, exportPageSize);

            // 全局页码计数器
            int globalPageNo = 1;

            for (int sheetIndex = 1; sheetIndex <= sheetCount; sheetIndex++) {
                WriteSheet writeSheet = buildWriteSheet(sheetIndex, clazz);
                
                int currentLoop = 0;
                while (currentLoop < loopCount) {
                    setPageNum(query, globalPageNo);
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
        } catch (Exception e) {
            log.warn("导出Excel文件异常", e);
            throw new BusinessException("导出Excel文件异常: " + e.getMessage());
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }

        return uploadFileToOss(fileName, file, downloadPath);
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
            log.warn("上传excel文件到oss服务器失败，返回本地路径。原因：{}", e.getMessage());
            return defaultPath;
        }
    }

    // --- Reflection Helpers ---

    private Integer getPageNum(V query) {
        return getIntProperty(query, "getPageNum");
    }

    private Integer getPageSize(V query) {
        return getIntProperty(query, "getPageSize");
    }
    
    private void setPageNum(V query, int pageNum) {
        setIntProperty(query, "setPageNum", pageNum);
    }

    private void setPageSize(V query, int pageSize) {
        setIntProperty(query, "setPageSize", pageSize);
    }

    private Integer getIntProperty(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            Object result = method.invoke(obj);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private void setIntProperty(Object obj, String methodName, int value) {
        try {
            try {
                Method method = obj.getClass().getMethod(methodName, Integer.class);
                method.invoke(obj, value);
            } catch (NoSuchMethodException e) {
                Method method = obj.getClass().getMethod(methodName, int.class);
                method.invoke(obj, value);
            }
        } catch (Exception e) {
             // Ignore
        }
    }
}
