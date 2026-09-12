-- ============================================================
-- 计数器下限加固：库存 / 名额 / 积分不允许为负（CHECK 约束）
-- 日期：2026-09-13
-- 背景：并发缺陷修复前，「取消报名」缺少条件更新（CAS）：
--       状态校验在活动行锁之外，5 个并发取消请求全部通过校验并各自释放一次名额，
--       实测把 sys_activity.current_num 从 1 打成 -4。
--       而满员判断是 current_num >= capacity —— 计数一旦为负，
--       名额上限直接失效（容量 5 的活动能被放进 9 人）。
--       应用层已改为「条件更新 + current_num > 0 守卫 / stock + delta >= 0 守卫」，
--       本脚本提供数据库层兜底：即使代码再退化，这些计数也不会变成负数。
-- 使用：仅首次执行一次。执行前必须先跑第 1 步检查，确认没有负值。
-- 说明：MySQL 8.0.16 起才真正校验 CHECK 约束（本机为 8.0.46）。
-- ============================================================

-- 1. 执行前检查：任何一行返回都说明已有脏数据，必须先做第 2 步再建约束
SELECT 'sys_goods.stock' AS column_name, goods_id AS row_id, stock AS bad_value
FROM sys_goods WHERE stock < 0
UNION ALL
SELECT 'sys_activity.current_num', activity_id, current_num
FROM sys_activity WHERE current_num < 0
UNION ALL
SELECT 'sys_user.current_points', user_id, current_points
FROM sys_user WHERE current_points < 0
UNION ALL
SELECT 'sys_user.total_points', user_id, total_points
FROM sys_user WHERE total_points < 0;

-- 2. 脏数据修复：把已经存在的负数夹到 0，让第 3 步能顺利建约束
--    注意：这一步会改写数据。执行前请确认这些负数确实来自并发缺陷，
--    而不是业务上真有「欠账」含义（当前业务没有这种含义）。
UPDATE sys_goods    SET stock = 0          WHERE stock < 0;
UPDATE sys_activity SET current_num = 0    WHERE current_num < 0;
UPDATE sys_user     SET current_points = 0 WHERE current_points < 0;
UPDATE sys_user     SET total_points = 0   WHERE total_points < 0;

-- 3. 建约束
ALTER TABLE sys_goods
    ADD CONSTRAINT ck_goods_stock_non_negative CHECK (stock >= 0);

ALTER TABLE sys_activity
    ADD CONSTRAINT ck_activity_current_num_non_negative CHECK (current_num >= 0);

ALTER TABLE sys_user
    ADD CONSTRAINT ck_user_current_points_non_negative CHECK (current_points >= 0),
    ADD CONSTRAINT ck_user_total_points_non_negative CHECK (total_points >= 0);

-- 4. 复核：约束应当出现在 SHOW CREATE TABLE 的输出里
SHOW CREATE TABLE sys_goods;
SHOW CREATE TABLE sys_activity;
SHOW CREATE TABLE sys_user;
