package com.volunteer.system.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 密码编码与兼容校验工具。
 * 新密码统一使用 BCrypt；旧的 32 位 MD5 仅用于登录兼容和自动升级。
 */
public final class PasswordUtils {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final Pattern MD5_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    private PasswordUtils() {
    }

    public static String encode(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            try {
                return BCRYPT.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        if (isLegacyMd5(storedPassword)) {
            String inputMd5 = DigestUtils.md5DigestAsHex(
                    rawPassword.getBytes(StandardCharsets.UTF_8));
            return storedPassword.equalsIgnoreCase(inputMd5);
        }

        // 不接受数据库中的明文或其他未知格式。
        return false;
    }

    public static boolean isLegacyMd5(String storedPassword) {
        return storedPassword != null && MD5_PATTERN.matcher(storedPassword).matches();
    }
}
