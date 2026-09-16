package com.volunteer.system.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 核销码生成器。
 *
 * 为什么把生成逻辑从兑换服务里抽出来：
 *   1. 随机源可注入 —— 「唯一键冲突 → 换码重试」这段逻辑只有把随机源换成固定序列才测得了
 *      （真实碰撞概率 32^-10 ≈ 1e-15，靠跑用例等不到）；
 *   2. 生成规则（前缀 / 字符集 / 长度）只在这里定义一处，兑换服务不再关心"码是怎么造出来的"。
 *
 * 码的形态：GIFT- + 10 位随机串，字符集去掉了 0/O、1/I 等易混字符，便于线下口头核对。
 * 组合数 32^10 ≈ 1.1e15，配合 sys_exchange_record.redeem_code 上的唯一索引，
 * 重复码会在数据库层被挡下（由调用方换码重试）。
 */
@Component
public class RedeemCodeGenerator {

    /** 兑换码字符集：去掉 0/O、1/I 等易混字符 */
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    /** 随机部分长度 */
    private static final int RANDOM_LENGTH = 10;
    /** 固定前缀 */
    private static final String PREFIX = "GIFT-";

    private final SecureRandom random;

    /** 生产用：持有独立的 SecureRandom 实例（线程安全） */
    public RedeemCodeGenerator() {
        this(new SecureRandom());
    }

    /** 测试用：注入可控随机源，便于构造"撞码"场景 */
    public RedeemCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    /**
     * 生成一个核销码。
     *
     * 注意：这里只保证「格式随机」，不查库、也不保证全局唯一 ——
     * 唯一性由数据库唯一索引兜底，调用方负责在冲突时换码重试。
     * 这样做的原因：先查后插本身又是「先查后改」的竞态，查一次库也换不来原子性。
     */
    public String next() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }
}
