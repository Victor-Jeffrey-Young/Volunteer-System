package com.volunteer.system.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 核销码生成器单元测试。
 *
 * 重点验证「随机源可注入」这件事：只有随机源可控，
 * 兑换服务里那段「撞唯一索引就换码重试」才测得了。
 */
class RedeemCodeGeneratorTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("^GIFT-[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{10}$");

    @Test
    @DisplayName("格式：GIFT- 前缀 + 10 位，字符集不含易混的 0/O/1/I")
    void next_hasExpectedFormat() {
        RedeemCodeGenerator generator = new RedeemCodeGenerator();

        for (int i = 0; i < 50; i++) {
            String code = generator.next();
            assertTrue(CODE_PATTERN.matcher(code).matches(), "核销码格式不符: " + code);
        }
    }

    @Test
    @DisplayName("随机性：连续生成的码不应重复（真随机源下 1000 个不重复）")
    void next_isRandomEnough() {
        RedeemCodeGenerator generator = new RedeemCodeGenerator();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            codes.add(generator.next());
        }

        assertEquals(1000, codes.size(), "真随机源下 1000 个码不应出现重复");
    }

    @Test
    @DisplayName("可注入：固定随机源产出确定性的码，供冲突重试用例使用")
    void next_isDeterministicWithInjectedRandom() {
        // 每次都返回下标 0 -> 字符集第一个字符 'A'
        RedeemCodeGenerator generator = new RedeemCodeGenerator(new FixedRandom(0));

        assertEquals("GIFT-AAAAAAAAAA", generator.next());
        assertEquals("GIFT-AAAAAAAAAA", generator.next());
    }

    @Test
    @DisplayName("可注入：随机源可以给出不同的码，模拟「换一个码」")
    void next_canProduceDifferentCodesWithSequence() {
        // 第一次全 0（A），之后全 1（B）
        RedeemCodeGenerator generator = new RedeemCodeGenerator(new SequencedRandom(10));

        assertEquals("GIFT-AAAAAAAAAA", generator.next());
        assertEquals("GIFT-BBBBBBBBBB", generator.next());
    }

    /** 固定返回同一个下标（模拟"每次生成同一个码"） */
    private static class FixedRandom extends SecureRandom {
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value % bound;
        }
    }

    /** 前 switchAfter 次返回 0，之后返回 1（模拟"先撞码、换一个码"） */
    private static class SequencedRandom extends SecureRandom {
        private final int switchAfter;
        private int calls = 0;

        SequencedRandom(int switchAfter) {
            this.switchAfter = switchAfter;
        }

        @Override
        public int nextInt(int bound) {
            return (calls++ < switchAfter ? 0 : 1) % bound;
        }
    }
}
