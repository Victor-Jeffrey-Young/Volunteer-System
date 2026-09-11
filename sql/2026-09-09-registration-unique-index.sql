-- ============================================================
-- 报名模块并发防护：sys_registration 唯一索引迁移脚本
-- 日期：2026-09-09
-- 背景：applyActivity 曾为先查后插、无锁读改写，且表上无任何唯一约束，
--       并发请求可插入重复有效报名、current_num 丢失更新导致名额超卖。
-- 修复：服务层悲观锁 (SysActivityMapper.selectByIdForUpdate) 为第一道防线，
--       本脚本为数据库兜底防线。
-- 方案：MySQL 唯一索引对 NULL 不生效。加生成列 active_flag：
--       status 属于有效状态 (0待审/1通过/3完结/5已签到/6已签退) 时为 1，
--       拒绝(2)/取消(4) 等历史记录为 NULL。对 (user_id, activity_id, active_flag)
--       建唯一索引后：同一用户对同一活动最多一条有效报名，
--       历史被拒/取消记录自然豁免，不影响“重新报名”业务。
-- 使用：仅首次执行一次。执行前需先清理存量重复有效报名
--       (见脚本末尾的检查查询，须返回空集)。
-- ============================================================

-- 1. 执行前检查：若返回行，必须先人工去重 (保留最早一条)，否则建索引会失败
SELECT user_id, activity_id, COUNT(*) AS cnt
FROM sys_registration
WHERE status IN (0, 1, 3, 5, 6)
GROUP BY user_id, activity_id
HAVING cnt > 1;

-- 2. 加生成列 (STORED：与业务查询同步维护，不受函数索引兼容性影响)
ALTER TABLE sys_registration
    ADD COLUMN active_flag TINYINT
    GENERATED ALWAYS AS (CASE WHEN status IN (0, 1, 3, 5, 6) THEN 1 ELSE NULL END) STORED;

-- 3. 建三列唯一索引：有效报名唯一，历史记录 (active_flag 为 NULL) 豁免
ALTER TABLE sys_registration
    ADD UNIQUE KEY uk_user_activity_active (user_id, activity_id, active_flag);

-- 4. 验证：应出现新列与新索引
SHOW COLUMNS FROM sys_registration LIKE 'active_flag';
SHOW INDEX FROM sys_registration WHERE Key_name = 'uk_user_activity_active';

-- 5. 兜底自测：单条语句插入两条重复有效报名，应整句报 1062 Duplicate entry
--    MySQL 多行 INSERT 是原子操作：冲突即整句失败，不会残留半截数据，无需清理
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, actual_hours)
SELECT 1, 1, 0, NOW(), 0
UNION ALL
SELECT 1, 1, 0, NOW(), 0;
-- 预期报错：Duplicate entry '1-1-1' for key 'uk_user_activity_active'
-- 报错即说明唯一索引兜底生效，脚本到此完成

