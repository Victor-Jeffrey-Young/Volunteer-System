package com.volunteer.system.controller;

import com.volunteer.system.common.Result;
import com.volunteer.system.common.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 * 负责处理系统中所有图片（如商品图片、头像等）的本地上传与存储。
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "07. 文件模块", description = "处理非结构化数据(图片)的上传与映射")
public class FileController {

    // 路径探测
    private static final String UPLOAD_PATH;

    static {
        String userDir = System.getProperty("user.dir");
        String testPath = userDir + "/files/";
        File folder = new File(testPath);
        if (!folder.exists() && new File(userDir + "/2.Backend/files/").exists()) {
            UPLOAD_PATH = userDir + "/2.Backend/files/";
        } else {
            UPLOAD_PATH = testPath;
        }
        log.info("【文件系统】当前存储物理路径设定为: {}", UPLOAD_PATH);
    }

    /**
     * 支持分类存储的上传接口
     * @param type 分类标签：avatar (头像), goods (商品), activity (活动)
     */
    @PostMapping("/upload")
    @Operation(summary = "分类上传图片", description = "支持图片合法性校验，并根据业务类型自动归档存储")
    public Result<String> upload(
            @Parameter(description = "二进制图片文件", required = true) @RequestParam MultipartFile file,
            @Parameter(description = "业务分类：avatar, goods, activity, common", example = "avatar") @RequestParam(defaultValue = "common") String type) {

        if (file == null || file.isEmpty()) throw new ServiceException(400, "文件为空");

        // 1. 安全过滤：限制文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ServiceException(400, "仅允许上传图片文件");
        }

        // 2. 动态目录构建：例如 /files/avatar/
        String subDir = type + "/";
        String finalUploadPath = UPLOAD_PATH + subDir;

        // 3. 自动创建目录
        File folder = new File(finalUploadPath);
        if (!folder.exists()) folder.mkdirs();

        // 4. 生成新文件名
        String fileName = UUID.randomUUID().toString().replace("-", "") +
                file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));

        try {
            file.transferTo(new File(finalUploadPath + fileName));

            // 返回给前端的 URL 必须带上子路径，如 /files/avatar/xxx.jpg
            return Result.success("/files/" + subDir + fileName);
        } catch (IOException e) {
            throw new ServiceException(500, "磁盘写入失败");
        }
    }
}