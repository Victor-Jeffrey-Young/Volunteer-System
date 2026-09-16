-- ============================================================
-- 精编演示数据（可重复执行：先清空业务表，再写入一套自洽的展示数据）
--
-- 用途：公网演示 / 面试现场展示。原来的开发库里混着 Test-10、VV1、test
--       这类调试痕迹，直接用会显得不专业。
--
-- 执行：bash scripts/reset-demo-data.sh          （推荐，会自动备份）
--       docker exec -i volunteer-mysql mysql -uroot -p*** volunteer_db < sql/demo-data.sql
--
-- 自洽性约束（脚本末尾有自检）：
--   * current_num = 该活动有效报名数（状态 0/1/3/5/6），由 UPDATE 统一校准；
--   * 用户积分 = 完结报名累计积分 + 已结算心愿奖励 − 已兑换消耗；
--   * 时间全部相对 NOW() 生成，任何时候导入都"新鲜"；
--   * 引用 /files/ 下真实存在的图片（随仓库入库）。
--
-- 演示账号（口令统一 Demo@123456）：
--   admin      管理员（发布活动/审核心愿/发工时/核销）
--   volunteer1 志愿者（32h+ 老手，有可续签到的进行中活动）
--   volunteer2 志愿者（新手，有"已签退待发工时"的记录）
--   resident1  居民（发布了覆盖 7 种状态的心愿）
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 0. 清空（无外键，顺序无关） ----------
TRUNCATE TABLE sys_registration;
TRUNCATE TABLE sys_exchange_record;
TRUNCATE TABLE sys_wish;
TRUNCATE TABLE sys_notice_read;
TRUNCATE TABLE sys_notice;
TRUNCATE TABLE sys_activity;
TRUNCATE TABLE sys_goods;
TRUNCATE TABLE sys_user;

-- ---------- 1. 用户 ----------
INSERT INTO sys_user
(username, password, real_name, gender, phone, email, avatar, role, total_hours, total_points,
 status, create_time, skills, available_time, current_points, last_rank, last_points_rank, last_hours_rank,
 like_count, dislike_count, neutral_count, likes)
VALUES
('admin', '$2a$10$fsv1WEWa/NeXAkVek4xj2uDVG29J1sRWYBmHymPqui7ogA4fqDHna',
 '系统管理员', 1, '13800000001', 'admin@demo.local', NULL, 'ADMIN',
 0.00, 0, 1, NOW() - INTERVAL 120 DAY, NULL, NULL, 0, 0, 0, 0, 0, 0, 0, 0),

('volunteer1', '$2a$10$hSreMaY4A/NDShD3xhr6GedkNGeqJ1QOosNMfVn13qhWjXlM9.jr2',
 '林晓雯', 2, '13800000002', 'volunteer1@demo.local', '/files/avatar/03790f000082418181ec2a88249c2bbe.jpg',
 'VOLUNTEER', 18.50, 185, 1, NOW() - INTERVAL 100 DAY,
 '["活动组织","应急救护","摄影"]', '周末全天', 125, 1, 1, 1, 12, 0, 0, 12),

('volunteer2', '$2a$10$8VwKwZ4frYSIXAV1sMrGueMO/TRs93C/yqEBwK8Oy6nOC1ZOci/Ka',
 '陈嘉豪', 1, '13800000003', 'volunteer2@demo.local', '/files/avatar/1a70102e71324770a59557c8da348aee.webp',
 'VOLUNTEER', 17.00, 180, 1, NOW() - INTERVAL 80 DAY,
 '["文案撰写","摄影","电脑维修"]', '工作日晚上', 150, 2, 2, 2, 7, 0, 0, 7),

('resident1', '$2a$10$R/TUd.I9PCevjvSBCUsauurtxRjgSv/G1gB2I5kXA8OMWlOtrdKHa',
 '周慧敏', 2, '13800000004', 'resident1@demo.local', '/files/avatar/472c603f46554adc8671e72ef8878d66.png',
 'RESIDENT', 0.00, 0, 1, NOW() - INTERVAL 60 DAY, NULL, '周末上午', 0, 0, 0, 0, 0, 0, 0, 0);

-- ---------- 2. 活动（1 个进行中用于现场演示签到，2 个招募中，6 个已结束） ----------
INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '社区环境清洁日',
       '对社区主干道、健身广场与绿化带进行集中清理，现场提供工具与饮用水。',
       '环境保护', '阳光社区中心广场',
       NOW() + INTERVAL 5 DAY, NOW() + INTERVAL 5 DAY + INTERVAL 3 HOUR, NOW() + INTERVAL 4 DAY,
       20, 0, 3.00, 0, u.user_id, NOW() - INTERVAL 12 DAY, '["无需特殊技能"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '敬老院陪伴服务', '陪老人聊天、读报、协助整理房间，需要耐心与沟通能力。',
       '助老服务', '幸福敬老院（社区东门对面）',
       NOW() + INTERVAL 8 DAY, NOW() + INTERVAL 8 DAY + INTERVAL 4 HOUR, NOW() + INTERVAL 7 DAY,
       12, 0, 4.00, 0, u.user_id, NOW() - INTERVAL 10 DAY, '["沟通能力"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '社区义诊协助', '协助社区医生登记信息、引导排队、发放健康宣传册。',
       '医疗支援', '社区卫生服务站',
       NOW() - INTERVAL 30 MINUTE, NOW() + INTERVAL 3 HOUR, NOW() - INTERVAL 1 DAY,
       10, 0, 3.00, 1, u.user_id, NOW() - INTERVAL 15 DAY, '["细心","有耐心"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '图书馆图书整理', '按分类上架新到图书，修补破损书刊。',
       '文化艺术', '社区图书馆',
       NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY + INTERVAL 6 HOUR, NOW() - INTERVAL 11 DAY,
       15, 0, 4.00, 2, u.user_id, NOW() - INTERVAL 20 DAY, '["细致"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '马拉松补给站志愿服务', '为参赛选手提供饮水与能量补给，协助维持赛道秩序。',
       '赛事服务', '滨江路 5 公里补给点',
       NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY + INTERVAL 8 HOUR, NOW() - INTERVAL 27 DAY,
       30, 0, 6.00, 2, u.user_id, NOW() - INTERVAL 35 DAY, '["体力良好"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '儿童课后辅导', '为社区双职工家庭的孩子辅导作业，重点语文与数学。',
       '教育助学', '社区四点半课堂',
       NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY + INTERVAL 4 HOUR, NOW() - INTERVAL 42 DAY,
       8, 0, 4.00, 2, u.user_id, NOW() - INTERVAL 50 DAY, '["语数基础"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '社区防疫宣传', '在小区出入口发放防疫手册，讲解季节性流感预防知识。',
       '社区服务', '阳光社区各出入口',
       NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 55 DAY + INTERVAL 5 HOUR, NOW() - INTERVAL 57 DAY,
       10, 0, 5.00, 2, u.user_id, NOW() - INTERVAL 65 DAY, '["沟通能力"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '旧衣回收整理', '对居民捐赠的旧衣物进行分类、打包，交由公益机构统一处理。',
       '环境保护', '社区党群服务中心',
       NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 70 DAY + INTERVAL 6 HOUR, NOW() - INTERVAL 72 DAY,
       12, 0, 6.00, 2, u.user_id, NOW() - INTERVAL 80 DAY, '["体力良好"]'
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_activity
(title, content, type, location, start_time, end_time, deadline, capacity, current_num,
 reward_hours, status, organizer_id, create_time, required_skills)
SELECT '暑期安全宣讲', '面向社区中小学生讲解防溺水、防触电与交通安全常识。',
       '教育助学', '阳光社区多功能厅',
       NOW() - INTERVAL 85 DAY, NOW() - INTERVAL 85 DAY + INTERVAL 5 HOUR, NOW() - INTERVAL 87 DAY,
       25, 0, 5.00, 2, u.user_id, NOW() - INTERVAL 95 DAY, '["表达能力"]'
FROM sys_user u WHERE u.username = 'admin';

-- ---------- 3. 报名（覆盖 0 待审 / 1 通过 / 2 拒绝 / 3 完结 / 4 取消 / 5 已签到 / 6 已签退） ----------
-- 招募中：一个已通过、一个待审
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, remarks, reward_points)
SELECT u.user_id, a.activity_id, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, 0.00, NULL, 0
FROM sys_user u JOIN sys_activity a ON a.title = '社区环境清洁日' WHERE u.username = 'volunteer1';
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, actual_hours, reward_points)
SELECT u.user_id, a.activity_id, 0, NOW() - INTERVAL 1 DAY, 0.00, 0
FROM sys_user u JOIN sys_activity a ON a.title = '社区环境清洁日' WHERE u.username = 'volunteer2';

-- 招募中：一个待审 + 一条被拒绝的历史（拒绝不占名额，可重新报名）
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, actual_hours, reward_points)
SELECT u.user_id, a.activity_id, 0, NOW() - INTERVAL 2 DAY, 0.00, 0
FROM sys_user u JOIN sys_activity a ON a.title = '敬老院陪伴服务' WHERE u.username = 'volunteer1';
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, remarks, reward_points)
SELECT u.user_id, a.activity_id, 2, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, 0.00,
       '该时段报名人数已满，感谢参与，欢迎报名下一场', 0
FROM sys_user u JOIN sys_activity a ON a.title = '敬老院陪伴服务' WHERE u.username = 'volunteer2';

-- 进行中：一个已签到（可现场演示签退）、一个已通过（可现场演示签到）
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, reward_points)
SELECT u.user_id, a.activity_id, 5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, 0.00, NOW() - INTERVAL 20 MINUTE, 0
FROM sys_user u JOIN sys_activity a ON a.title = '社区义诊协助' WHERE u.username = 'volunteer1';
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, reward_points)
SELECT u.user_id, a.activity_id, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, 0.00, 0
FROM sys_user u JOIN sys_activity a ON a.title = '社区义诊协助' WHERE u.username = 'volunteer2';

-- 已结束：一个完结（+40 分） + 一个已签退待发工时（可现场演示"一键发工时"）
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 13 DAY, 4.00,
       NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY + INTERVAL 4 HOUR, 40
FROM sys_user u JOIN sys_activity a ON a.title = '图书馆图书整理' WHERE u.username = 'volunteer1';
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 6, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 13 DAY, 0.00,
       NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY + INTERVAL 4 HOUR, 0
FROM sys_user u JOIN sys_activity a ON a.title = '图书馆图书整理' WHERE u.username = 'volunteer2';

-- 已结束：一条取消记录（历史留痕，不占名额）
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, actual_hours, remarks, reward_points)
SELECT u.user_id, a.activity_id, 4, NOW() - INTERVAL 30 DAY, 0.00, '临时出差，无法参加', 0
FROM sys_user u JOIN sys_activity a ON a.title = '马拉松补给站志愿服务' WHERE u.username = 'volunteer1';
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 29 DAY, 6.00,
       NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY + INTERVAL 6 HOUR, 60
FROM sys_user u JOIN sys_activity a ON a.title = '马拉松补给站志愿服务' WHERE u.username = 'volunteer2';

-- 其余已结束活动：volunteer1 三次完结、volunteer2 两次完结（积分与总账一致）
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 44 DAY, NOW() - INTERVAL 43 DAY, 3.50,
       NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY + INTERVAL 4 HOUR, 35
FROM sys_user u JOIN sys_activity a ON a.title = '儿童课后辅导' WHERE u.username = 'volunteer1';

INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 59 DAY, NOW() - INTERVAL 58 DAY, 5.00,
       NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 55 DAY + INTERVAL 5 HOUR, 50
FROM sys_user u JOIN sys_activity a ON a.title = '社区防疫宣传' WHERE u.username = 'volunteer1';

INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 74 DAY, NOW() - INTERVAL 73 DAY, 6.00,
       NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 70 DAY + INTERVAL 6 HOUR, 60
FROM sys_user u JOIN sys_activity a ON a.title = '旧衣回收整理' WHERE u.username = 'volunteer1';
INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 74 DAY, NOW() - INTERVAL 73 DAY, 4.00,
       NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 70 DAY + INTERVAL 4 HOUR, 40
FROM sys_user u JOIN sys_activity a ON a.title = '旧衣回收整理' WHERE u.username = 'volunteer2';

INSERT INTO sys_registration (user_id, activity_id, status, apply_time, audit_time, actual_hours, sign_in_time, sign_out_time, reward_points)
SELECT u.user_id, a.activity_id, 3, NOW() - INTERVAL 89 DAY, NOW() - INTERVAL 88 DAY, 5.00,
       NOW() - INTERVAL 85 DAY, NOW() - INTERVAL 85 DAY + INTERVAL 5 HOUR, 50
FROM sys_user u JOIN sys_activity a ON a.title = '暑期安全宣讲' WHERE u.username = 'volunteer2';

-- ---------- 4. 积分商城 ----------
INSERT INTO sys_goods (name, description, points_required, stock, image, create_time, category) VALUES
('环保帆布袋', '社区定制款，加厚面料，日常买菜购物都能用', 60, 20, '/files/common/deb28bc934864b368771291f50c3b8bb.png', NOW() - INTERVAL 60 DAY, '生活用品'),
('不锈钢保温杯', '316 不锈钢内胆，保温 12 小时，志愿者专属刻字', 120, 8, '/files/common/b68f3206eb884d16832b871806855a52.png', NOW() - INTERVAL 55 DAY, '生活用品'),
('志愿者纪念徽章', '年度志愿服务纪念款，带独立编号', 30, 50, '/files/common/3cae1af9e0784f77a15d73bf40183621.png', NOW() - INTERVAL 50 DAY, '纪念品'),
('定制笔记本', 'A5 硬壳本，内含志愿服务记录页', 45, 25, '/files/common/bae80d2540184f42987b42a1eb377b7e.png', NOW() - INTERVAL 45 DAY, '纪念品'),
('无线鼠标', '静音款，2.4G 无线连接，办公学习皆可', 200, 3, '/files/common/4ee01487b1d444a884070557ec8cbb01.png', NOW() - INTERVAL 40 DAY, '电子产品'),
('折叠雨伞', '八骨加固，晴雨两用（当前已兑完，可现场演示"库存不足"）', 90, 0, NULL, NOW() - INTERVAL 35 DAY, '生活用品');

-- ---------- 5. 兑换记录（一条已核销、一条待核销，便于演示扫码核销） ----------
INSERT INTO sys_exchange_record (user_id, goods_id, cost_points, status, create_time, redeem_code, exchange_time)
SELECT u.user_id, g.goods_id, g.points_required, 1, NOW() - INTERVAL 6 DAY, 'GIFT-DEMO000001', NOW() - INTERVAL 5 DAY
FROM sys_user u JOIN sys_goods g ON g.name = '环保帆布袋' WHERE u.username = 'volunteer1';

INSERT INTO sys_exchange_record (user_id, goods_id, cost_points, status, create_time, redeem_code, exchange_time)
SELECT u.user_id, g.goods_id, g.points_required, 0, NOW() - INTERVAL 2 DAY, 'GIFT-DEMO000002', NULL
FROM sys_user u JOIN sys_goods g ON g.name = '志愿者纪念徽章' WHERE u.username = 'volunteer2';

-- ---------- 6. 微心愿（覆盖 7 种状态，可现场走完整条状态机） ----------
INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, NULL, '厨房水龙头一直滴水', '水龙头关不紧，一直滴水，希望有懂维修的志愿者帮忙看看。',
       '日常维修', '阳光社区 3 栋 2 单元 501', 0, 20, 1.00, NOW() - INTERVAL 6 HOUR, NULL, NULL,
       NULL, NULL, 0, 0
FROM sys_user r WHERE r.username = 'resident1';

INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, NULL, '想请人陪我母亲去医院复查', '老人腿脚不便，复查需要有人陪同挂号、取药，半天时间。',
       '助老/医疗', '市第一人民医院', 1, 30, 3.00, NOW() - INTERVAL 2 DAY, NULL,
       '已核实情况属实，欢迎志愿者认领', NULL, NULL, 0, 0
FROM sys_user r WHERE r.username = 'resident1';

INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, v.user_id, '帮独居老人代买米面油', '老人行动不便，需要每月代买一次生活物资，费用由我承担。',
       '跑腿代办', '阳光社区 7 栋 1 单元 302', 2, 20, 2.00, NOW() - INTERVAL 4 DAY, NULL,
       '已核实情况属实，欢迎志愿者认领', NULL, NULL, 0, 0
FROM sys_user r JOIN sys_user v ON v.username = 'volunteer1' WHERE r.username = 'resident1';

INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, v.user_id, '教我用手机挂号', '想学会自己在手机上预约挂号，不用每次都麻烦孩子。',
       '助老/医疗', '阳光社区 5 栋 1 单元 101', 5, 15, 1.50, NOW() - INTERVAL 3 DAY, NULL,
       '已核实情况属实，欢迎志愿者认领', NULL, NULL, 0, 0
FROM sys_user r JOIN sys_user v ON v.username = 'volunteer2' WHERE r.username = 'resident1';

INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, v.user_id, '搬运旧家具到回收点', '家里换家具，旧沙发和柜子需要搬到小区回收点，两个人抬得动。',
       '跑腿代办', '阳光社区 2 栋 3 单元 601', 6, 25, 2.00, NOW() - INTERVAL 5 DAY, NULL,
       '已核实情况属实，欢迎志愿者认领', NULL, NULL, 0, 0
FROM sys_user r JOIN sys_user v ON v.username = 'volunteer1' WHERE r.username = 'resident1';

INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, v.user_id, '陪老人聊天下棋', '独居多年，希望每周有人来下下棋、说说话。',
       '心理疏导', '阳光社区 6 栋 2 单元 203', 3, 30, 2.00, NOW() - INTERVAL 20 DAY,
       NOW() - INTERVAL 14 DAY, '已核实情况属实，欢迎志愿者认领', 3, '小伙子很有耐心，老人很开心', 0, 1
FROM sys_user r JOIN sys_user v ON v.username = 'volunteer2' WHERE r.username = 'resident1';

INSERT INTO sys_wish (requester_id, volunteer_id, title, content, category, address, status,
                      reward_points, reward_hours, create_time, finish_time, remarks,
                      evaluation, evaluation_remarks, eval_status, is_liked)
SELECT r.user_id, NULL, '想要一台新手机', '手机有点卡，希望平台能送一台新手机。',
       '其他', '阳光社区 3 栋 2 单元 501', 4, 0, 0.00, NOW() - INTERVAL 9 DAY, NULL,
       '心愿内容超出社区志愿服务范围，建议通过其他渠道解决', NULL, NULL, 0, 0
FROM sys_user r WHERE r.username = 'resident1';

-- ---------- 7. 公告 ----------
INSERT INTO sys_notice (title, content, type, publisher_id, create_time)
SELECT '关于开展社区环境清洁日活动的通知',
       '本周六上午 9:00 在社区中心广场集合，开展主干道与绿化带集中清理。现场提供工具、手套与饮用水，欢迎志愿者报名参加。',
       0, u.user_id, NOW() - INTERVAL 3 DAY
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_notice (title, content, type, publisher_id, create_time)
SELECT '积分商城新上架：环保帆布袋、不锈钢保温杯',
       '本月新增两款兑换商品，志愿者可凭可用积分在"积分商城"兑换，兑换后凭核销码到社区服务中心领取。',
       0, u.user_id, NOW() - INTERVAL 5 DAY
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_notice (title, content, type, publisher_id, create_time)
SELECT '志愿服务时长认证说明',
       '活动结束后由管理员核发实际服务时长，累计时长可在"个人中心"查看；如需开具志愿服务证明，请联系社区工作人员。',
       0, u.user_id, NOW() - INTERVAL 12 DAY
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_notice (title, content, type, publisher_id, create_time)
SELECT '平台维护公告',
       '本周日凌晨 2:00-4:00 进行系统维护，期间可能无法访问，请提前安排报名与签退操作。',
       1, u.user_id, NOW() - INTERVAL 1 DAY
FROM sys_user u WHERE u.username = 'admin';

-- ---------- 8. 校准：已报名人数 = 有效报名数（保证演示数据自洽） ----------
UPDATE sys_activity a
SET a.current_num = (
    SELECT COUNT(*) FROM sys_registration r
    WHERE r.activity_id = a.activity_id AND r.status IN (0, 1, 3, 5, 6)
);

-- ---------- 9. 自检输出 ----------
SELECT '演示数据导入完成' AS 结果;
SELECT '用户' AS 表, COUNT(*) AS 行数 FROM sys_user
UNION ALL SELECT '活动', COUNT(*) FROM sys_activity
UNION ALL SELECT '报名', COUNT(*) FROM sys_registration
UNION ALL SELECT '商品', COUNT(*) FROM sys_goods
UNION ALL SELECT '兑换记录', COUNT(*) FROM sys_exchange_record
UNION ALL SELECT '心愿', COUNT(*) FROM sys_wish
UNION ALL SELECT '公告', COUNT(*) FROM sys_notice;

-- 自检 1：有没有活动的 current_num 与有效报名数对不上（应为 0 行）
SELECT a.activity_id, a.title, a.current_num AS 计数, COUNT(r.reg_id) AS 实际
FROM sys_activity a
LEFT JOIN sys_registration r ON r.activity_id = a.activity_id AND r.status IN (0, 1, 3, 5, 6)
GROUP BY a.activity_id, a.title, a.current_num
HAVING a.current_num <> COUNT(r.reg_id);

-- 自检 2：有没有用户的可用积分超过累计积分（应为 0 行）
SELECT user_id, username, current_points, total_points
FROM sys_user WHERE current_points > total_points;

-- 自检 3：累计积分与工时 = 完结报名累计 + 已结算心愿奖励（应为 0 行）
SELECT u.user_id, u.username, u.total_points AS 累计积分, u.total_hours AS 累计工时,
       IFNULL(r.pts, 0) + IFNULL(w.pts, 0) AS 应为积分,
       IFNULL(r.hrs, 0) + IFNULL(w.hrs, 0) AS 应为工时
FROM sys_user u
LEFT JOIN (SELECT user_id, SUM(reward_points) pts, SUM(actual_hours) hrs
             FROM sys_registration WHERE status = 3 GROUP BY user_id) r ON r.user_id = u.user_id
LEFT JOIN (SELECT volunteer_id, SUM(reward_points) pts, SUM(reward_hours) hrs
             FROM sys_wish WHERE status = 3 GROUP BY volunteer_id) w ON w.volunteer_id = u.user_id
WHERE u.role = 'VOLUNTEER'
  AND (u.total_points <> IFNULL(r.pts, 0) + IFNULL(w.pts, 0)
    OR u.total_hours  <> IFNULL(r.hrs, 0) + IFNULL(w.hrs, 0));
