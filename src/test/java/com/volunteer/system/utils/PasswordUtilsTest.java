package com.volunteer.system.utils;

import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    @Test
    void bcryptPassword_RoundTrips() {
        String encoded = PasswordUtils.encode("correct-password");

        assertTrue(encoded.startsWith("$2"));
        assertTrue(PasswordUtils.matches("correct-password", encoded));
        assertFalse(PasswordUtils.matches("wrong-password", encoded));
    }

    @Test
    void legacyMd5_IsAcceptedForMigration() {
        String md5 = DigestUtils.md5DigestAsHex(
                "correct-password".getBytes(StandardCharsets.UTF_8));

        assertTrue(PasswordUtils.isLegacyMd5(md5));
        assertTrue(PasswordUtils.matches("correct-password", md5));
        assertFalse(PasswordUtils.matches("wrong-password", md5));
    }

    @Test
    void plaintextAndUnknownFormats_AreRejected() {
        assertFalse(PasswordUtils.matches("123456", "123456"));
        assertFalse(PasswordUtils.matches("123456", "$2a$malformed"));
        assertFalse(PasswordUtils.matches("123456", "$2a$99$" + "A".repeat(53)));
        assertFalse(PasswordUtils.matches("123456", null));
    }
}
