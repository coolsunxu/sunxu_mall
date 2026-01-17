package com.example.sunxu_mall.util;

import com.example.sunxu_mall.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author sunxu
 * @version 1.0
 * @date 2026/1/10 21:07
 * @description
 */

@Slf4j
public class FileUtil {
    public static MultipartFile toMultipartFile(String fileName, File file) {
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
        String contentType = new MimetypesFileTypeMap().getContentType(file);
        FileItem fileItem = diskFileItemFactory.createItem(fileName, contentType, false, file.getName());
        try (
                InputStream inputStream = new FileInputStream(file);
                OutputStream outputStream = fileItem.getOutputStream()
        ) {
            FileCopyUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("File to MultipartFile conversion failed, reason: ", e);
            throw new BusinessException("File转换MultipartFile失败");
        }
        return new CommonsMultipartFile(fileItem);
    }
}
