package com.volunteer.system.controller;

import com.volunteer.system.common.Result;
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

    // 设定文件存储的本地物理路径 (存在当前项目根目录下的 files 文件夹中)
    // System.getProperty("user.dir") 获取当前项目的绝对路径
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/files/";

    /**
     * 单文件上传接口
     * 接收前端传来的 MultipartFile，保存至本地磁盘，并返回可访问的 HTTP 绝对路径。
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片文件", description = "将文件保存到本地 /files 目录，并返回公网访问URL")
    public Result<String> upload(
            @Parameter(description = "表单文件对象", required = true) MultipartFile file,
            HttpServletRequest request) { // 注入 request 用于动态获取当前服务器的 IP 和端口

        if (file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }

        // 1. 获取原始文件名并提取后缀 (例如: .png, .jpg)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";

        // 2. 生成基于 UUID 的唯一文件名，防止不同用户上传同名文件导致覆盖
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 3. 检查并创建物理存储目录
        File dir = new File(UPLOAD_PATH);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("本地 files 目录不存在，执行创建: {}", created);
        }

        try {
            // 4. 保存文件到本地磁盘
            file.transferTo(new File(UPLOAD_PATH + newFileName));
            log.info("文件上传成功，已保存至本地: {}", UPLOAD_PATH + newFileName);

            // 5. 终极修复：直接返回相对路径！
            // 前端拿到 "/files/xxx.png" 后，会自动向当前域名发起请求，然后被 Vite Proxy 拦截并转发给后端
            String url = "/files/" + newFileName;

            return Result.success(url);

        } catch (IOException e) {
            log.error("文件读写发生异常: ", e);
            return Result.error(500, "上传失败: 服务器磁盘读写错误");
        }
    }
}