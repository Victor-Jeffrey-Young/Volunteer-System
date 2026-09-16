package com.volunteer.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 上传根目录的统一解析处。
 *
 * 为什么要抽出来：上传（FileController 写文件）与访问（WebConfig 把 /files/** 映射到磁盘）
 * 必须指向同一个目录，否则会出现"上传成功但图片 404"。而这个目录在三种环境里各不相同：
 *   - 本地开发：项目根目录下的 files/
 *   - 容器/服务器：由 app.upload.dir 指定的绝对路径（例如 /data/files，映射到宿主机卷）
 *   - 早期遗留：老仓库把文件放在 2.Backend/files/
 *
 * 以前这段探测逻辑写死在 FileController 的静态块里，WebConfig 又各自拼了一遍
 * user.dir —— 一旦用 systemd 或容器启动（工作目录不是项目根），两边就会不一致。
 * 现在统一由配置驱动，缺省时才回落到本地探测。
 */
@Slf4j
@Component
public class UploadProperties {

    private final String rootDir;

    public UploadProperties(@Value("${app.upload.dir:}") String configuredDir) {
        this.rootDir = (configuredDir == null || configuredDir.isBlank())
                ? detectLocalDir()
                : withTrailingSeparator(configuredDir.trim());
        log.info("【文件系统】当前存储物理路径设定为: {}", rootDir);
    }

    /** 上传根目录，末尾带路径分隔符，便于直接拼接子目录 */
    public String getRootDir() {
        return rootDir;
    }

    /** 未显式配置时的本地探测：优先 ./files/，兼容老仓库的 2.Backend/files/ */
    private static String detectLocalDir() {
        String userDir = System.getProperty("user.dir");
        String primary = userDir + File.separator + "files" + File.separator;
        String legacy = userDir + File.separator + "2.Backend" + File.separator + "files" + File.separator;
        if (!new File(primary).exists() && new File(legacy).exists()) {
            log.warn("【文件系统】未找到 {}，回落到旧目录 {}", primary, legacy);
            return legacy;
        }
        return primary;
    }

    private static String withTrailingSeparator(String dir) {
        return dir.endsWith("/") || dir.endsWith(File.separator) ? dir : dir + File.separator;
    }
}
