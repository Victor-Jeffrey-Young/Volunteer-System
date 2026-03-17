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

    // 设定文件存储的本地物理路径
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/files/";

    /**
     * 单文件上传接口
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片文件", description = "将文件保存到本地 /files 目录，并返回公网访问URL")
    public Result<String> upload(
            @Parameter(description = "表单文件对象", required = true) MultipartFile file,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            throw new ServiceException(400, "请选择要上传的文件");
        }

        // 1. 获取后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";

        // 2. 生成 UUID 文件名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 3. 检查目录
        File dir = new File(UPLOAD_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            // 4. 保存文件
            file.transferTo(new File(UPLOAD_PATH + newFileName));
            log.info("文件上传成功: {}", newFileName);

            // 5. 返回相对路径，由 Vite Proxy 转发访问
            String url = "/files/" + newFileName;
            return Result.success(url);

        } catch (IOException e) {
            log.error("文件读写发生异常: ", e);
            // 🚨 将 IO 异常包装为业务异常抛出，由全局异常处理器统一处理
            throw new ServiceException(500, "上传失败：服务器磁盘读写错误");
        }
    }
}