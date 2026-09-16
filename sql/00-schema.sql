-- ============================================================
-- 建表基线：全新数据库从这里开始
--
-- 执行顺序（重要）：本文件 → 三个日期迁移脚本（按日期先后）
--   mysql -uroot -p volunteer_db < sql/00-schema.sql
--   mysql -uroot -p volunteer_db < sql/2026-09-09-registration-unique-index.sql
--   mysql -uroot -p volunteer_db < sql/2026-09-12-redeem-code-unique-index.sql
--   mysql -uroot -p volunteer_db < sql/2026-09-13-counter-non-negative-check.sql
--
-- 为什么拆成"基线 + 迁移"而不是一份大 DDL：三个迁移脚本是**防线**，
-- 拆开才能在 CI 里真的执行一遍（漏跑就没有唯一索引与 CHECK 兜底）。
--   2026-09-09：sys_registration 生成列 active_flag + 唯一索引 uk_user_activity_active
--   2026-09-12：sys_exchange_record 唯一索引 uk_redeem_code
--   2026-09-13：库存 / 名额 / 积分的 CHECK 约束（不得为负）
--
-- 本文件是幂等的（CREATE TABLE IF NOT EXISTS），重复执行不会报错；
-- 但迁移脚本里是 ALTER TABLE，重复执行会因"索引/约束已存在"报错 —— 属预期。
-- 由 mysqldump --no-data 导出后剥掉上述迁移产物，字符集 utf8mb4。
-- ============================================================

SET NAMES utf8mb4;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_activity` (
  `activity_id` bigint NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `title` varchar(255) NOT NULL COMMENT '活动标题',
  `content` text COMMENT '活动内容/详情',
  `type` varchar(50) DEFAULT NULL COMMENT '活动类型 (社区服务、环境保护、支教等)',
  `location` varchar(255) DEFAULT NULL COMMENT '活动地点',
  `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
  `deadline` datetime DEFAULT NULL COMMENT '报名截止时间',
  `capacity` int DEFAULT '0' COMMENT '招募人数上限',
  `current_num` int DEFAULT '0' COMMENT '当前已报名成功人数',
  `reward_hours` decimal(10,2) DEFAULT '0.00' COMMENT '活动固定时长奖励',
  `status` tinyint DEFAULT '0' COMMENT '状态 (0-招募中, 1-进行中, 2-已结束, 3-已取消)',
  `organizer_id` bigint DEFAULT NULL COMMENT '发布人ID (关联用户表)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `required_skills` varchar(500) DEFAULT NULL COMMENT '所需技能 (JSON数组存储)',
  PRIMARY KEY (`activity_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='志愿活动表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_exchange_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `goods_id` bigint NOT NULL,
  `cost_points` int NOT NULL COMMENT '兑换时消耗的积分',
  `status` tinyint DEFAULT '0' COMMENT '核销状态(0-未核销, 1-已核销)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `redeem_code` varchar(20) DEFAULT NULL COMMENT '核销验证码(唯一)',
  `exchange_time` datetime DEFAULT NULL COMMENT '线下实际领取/核销时间',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_goods` (
  `goods_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `description` varchar(255) DEFAULT NULL COMMENT '商品描述',
  `points_required` int NOT NULL COMMENT '所需积分',
  `stock` int DEFAULT '0' COMMENT '库存',
  `image` varchar(255) DEFAULT NULL COMMENT '商品图片URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `category` varchar(255) DEFAULT '未分类' COMMENT '商品分类',
  PRIMARY KEY (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分商城商品表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(255) NOT NULL COMMENT '公告标题',
  `content` longtext COMMENT '公告内容',
  `type` tinyint DEFAULT '1' COMMENT '类型 (1-通知, 2-新闻)',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='新闻公告表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_notice_read` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_notice` (`user_id`,`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告阅读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_registration` (
  `reg_id` bigint NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `user_id` bigint NOT NULL COMMENT '志愿者ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `status` tinyint DEFAULT '0' COMMENT '报名状态 (0-待审核, 1-报名成功, 2-已拒绝, 3-已签到, 4-已完成/工时已发放)',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报名申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `actual_hours` decimal(10,2) DEFAULT '0.00' COMMENT '实际获得的工时',
  `remarks` varchar(255) DEFAULT NULL COMMENT '审核备注/不通过理由',
  `sign_in_time` datetime DEFAULT NULL COMMENT '签到打卡时间',
  `sign_out_time` datetime DEFAULT NULL COMMENT '签退打卡时间',
  `reward_points` int DEFAULT '0' COMMENT '本次活动获得的积分',
  PRIMARY KEY (`reg_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='志愿活动报名与工时记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '登录密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `gender` tinyint DEFAULT '0' COMMENT '性别 (0-未知, 1-男, 2-女)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `role` varchar(20) DEFAULT 'VOLUNTEER' COMMENT '角色 (ADMIN-管理员, VOLUNTEER-志愿者)',
  `total_hours` decimal(10,2) DEFAULT '0.00' COMMENT '累计志愿总时长',
  `total_points` int DEFAULT '0' COMMENT '累计总积分(决定段位)',
  `status` tinyint DEFAULT '1' COMMENT '帐号状态 (1-正常, 0-禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `skills` varchar(255) DEFAULT NULL COMMENT '技能特长(JSON存储)',
  `available_time` varchar(50) DEFAULT NULL COMMENT '空闲时间段',
  `current_points` int DEFAULT '0' COMMENT '当前可用积分(用于商城兑换)',
  `last_rank` int DEFAULT '0' COMMENT '昨日排名 (0表示无记录)',
  `last_points_rank` int DEFAULT '0',
  `last_hours_rank` int DEFAULT '0',
  `like_count` int DEFAULT '0' COMMENT '点赞数(棒)',
  `dislike_count` int DEFAULT '0' COMMENT '点踩数(踩)',
  `neutral_count` int DEFAULT '0' COMMENT '一般数(一般)',
  `likes` int DEFAULT '0',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_wish` (
  `wish_id` bigint NOT NULL AUTO_INCREMENT COMMENT '心愿ID',
  `requester_id` bigint NOT NULL COMMENT '发起人(居民/用户)ID',
  `volunteer_id` bigint DEFAULT NULL COMMENT '认领人(志愿者)ID',
  `title` varchar(100) NOT NULL COMMENT '心愿简要标题',
  `content` text NOT NULL COMMENT '详细描述/地址/背景信息',
  `category` varchar(50) DEFAULT '其他' COMMENT '心愿分类(跑腿、维修、陪伴、教育、咨询)',
  `address` varchar(255) DEFAULT NULL COMMENT '具体服务地点(如: X号楼X室)',
  `status` int DEFAULT '0' COMMENT '状态：0-审核中, 1-展示中(待认领), 2-进行中(已认领), 3-已完成, 4-已拒绝',
  `reward_points` int DEFAULT '0' COMMENT '完成后可获得的积分',
  `reward_hours` decimal(10,1) DEFAULT '0.0' COMMENT '完成后可核发的志愿工时',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `finish_time` datetime DEFAULT NULL COMMENT '心愿达成结算时间',
  `remarks` varchar(255) DEFAULT NULL COMMENT '审核反馈理由',
  `evaluation` int DEFAULT '0' COMMENT '评价: 1-踩, 2-一般, 3-棒',
  `evaluation_remarks` varchar(500) DEFAULT NULL COMMENT '居民评价备注',
  `eval_status` int DEFAULT '0' COMMENT '评价审核状态: 0-无需审核, 1-待审核(针对踩), 2-通过, 3-驳回',
  `is_liked` int DEFAULT '0',
  PRIMARY KEY (`wish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区微心愿管理表';
/*!40101 SET character_set_client = @saved_cs_client */;
