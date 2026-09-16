package com.volunteer.system.controller;

import com.volunteer.system.common.Result;
import com.volunteer.system.common.ServiceException;
import com.volunteer.system.config.UploadProperties;
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

    /**
     * 上传根目录由 UploadProperties 统一解析（配置 app.upload.dir，缺省回落到 ./files/）。
     * 以前这里是静态块里读 user.dir —— 换成 systemd/容器启动（工作目录不是项目根）时，
     * 上传会写到别处，而 WebConfig 又把 /files/** 映射到另一个目录，图片就 404。
     */
    private final UploadProperties uploadProperties;

    public FileController(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
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

        // 1. 安全过滤：限制文件类型（只信 Content-Type 是不够的，但先挡掉明显的非图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ServiceException(400, "仅允许上传图片文件");
        }

        // 2. 分类目录必须来自白名单。
        //    早期实现直接把客户端传的 type 拼进路径（UPLOAD_PATH + type + "/"），
        //    传 type=../../.. 就能把文件写到 web 根目录之外，是典型的路径穿越。
        String subDir = resolveSubDir(type);

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);

        // 3. 自动创建目录并校验最终路径仍落在上传根目录内（双保险）
        String finalUploadPath = uploadProperties.getRootDir() + subDir + "/";
        File folder = new File(finalUploadPath);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new ServiceException(500, "上传目录创建失败");
        }

        // 4. 生成新文件名：完全丢弃用户提供的文件名，只保留经白名单校验的扩展名
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        File target = new File(folder, fileName);
        if (!target.getAbsoluteFile().toPath().normalize()
                .startsWith(new File(uploadProperties.getRootDir()).getAbsoluteFile().toPath().normalize())) {
            throw new ServiceException(400, "非法的存储路径");
        }

        try {
            file.transferTo(target);

            // 返回给前端的 URL 必须带上子路径，如 /files/avatar/xxx.jpg
            return Result.success("/files/" + subDir + "/" + fileName);
        } catch (IOException e) {
            log.error("文件写入失败: {}", e.getMessage());
            throw new ServiceException(500, "磁盘写入失败");
        }
    }

    /** 允许的分类目录白名单 */
    private static final java.util.Set<String> ALLOWED_TYPES =
            java.util.Set.of("avatar", "goods", "activity", "common");

    /** 允许的图片扩展名白名单 */
    private static final java.util.Set<String> ALLOWED_EXTENSIONS =
            java.util.Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    /** 把客户端传入的分类收敛到白名单内的固定值，非法值一律落到 common */
    private String resolveSubDir(String type) {
        if (type == null) return "common";
        String normalized = type.trim().toLowerCase();
        return ALLOWED_TYPES.contains(normalized) ? normalized : "common";
    }

    /** 只从原始文件名里取扩展名，并做白名单校验；没有扩展名或类型不合法时报错 */
    private String extractExtension(String originalName) {
        if (originalName == null) {
            throw new ServiceException(400, "文件名不能为空");
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0) {
            throw new ServiceException(400, "文件缺少扩展名");
        }
        String extension = originalName.substring(dot).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ServiceException(400, "仅支持 jpg / jpeg / png / gif / webp / bmp 格式");
        }
        return extension;
    }
}