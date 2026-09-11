-- ============================================================
-- 核销码安全加固：sys_exchange_record.redeem_code 唯一索引
-- 日期：2026-09-12
-- 背景：旧实现用 "GIFT-" + (System.currentTimeMillis() % 10000) + "-" + userId
--       生成核销码，每个用户只有一万种可能，且 userId 直接印在码里，
--       可被枚举伪造后冒领商品；表上也没有唯一约束做兜底。
-- 修复：代码层改用 SecureRandom 生成 10 位随机码（32^10 ≈ 1.1e15 种可能），
--       本脚本提供数据库层兜底 —— 即使代码再退化，也不可能存在两个相同的码。
-- 使用：仅首次执行一次。执行前必须先跑第 1 步检查，确认没有重复值。
-- ============================================================

-- 1. 执行前检查：若返回行，必须先人工重发新码，否则建索引会失败
SELECT redeem_code, COUNT(*) AS cnt
FROM sys_exchange_record
GROUP BY redeem_code
HAVING cnt > 1;

-- 2. 建唯一索引
ALTER TABLE sys_exchange_record
    ADD UNIQUE KEY uk_redeem_code (redeem_code);

-- 3. 验证：应出现 uk_redeem_code 且 Non_unique = 0
SHOW INDEX FROM sys_exchange_record WHERE Key_name = 'uk_redeem_code';

-- 4. 兜底自测：尝试插入两条相同核销码，应报 1062 Duplicate entry
-- INSERT INTO sys_exchange_record (user_id, goods_id, cost_points, redeem_code, status)
-- VALUES (1, 1, 0, 'GIFT-DUPLICATETEST', 0), (1, 1, 0, 'GIFT-DUPLICATETEST', 0);
-- 预期报错：Duplicate entry 'GIFT-DUPLICATETEST' for key 'uk_redeem_code'
