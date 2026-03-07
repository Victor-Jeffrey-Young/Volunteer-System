package com.volunteer.volunteersystem.controller;

import com.volunteer.volunteersystem.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // 设定文件存储的本地物理路径 (这里存在项目运行目录下的 files 文件夹中)
    // System.getProperty("user.dir") 获取当前项目根目录
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/files/";

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择文件");
        }

        // 1. 生成唯一文件名，防止重名覆盖 (例如: uuid.png)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 2. 创建文件夹 (如果不存在)
        File dir = new File(UPLOAD_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            // 3. 保存文件到本地磁盘
            file.transferTo(new File(UPLOAD_PATH + newFileName));

            // 4. 返回可访问的 URL 地址
            // 假设后端端口是 8080，我们映射一个虚拟路径 /files/
            String url = "http://localhost:8080/files/" + newFileName;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }
}