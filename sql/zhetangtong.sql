-- ============================================================
-- 蔗糖通 · 蔗糖收购结算平台  建库建表 + 演示种子数据
-- 可重复执行：内部先 DROP 再 CREATE
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < sql/zhetangtong.sql
-- 演示数据均为脱敏假名假电话，无真实隐私信息
-- ============================================================

DROP DATABASE IF EXISTS zhetangtong;
CREATE DATABASE zhetangtong DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE zhetangtong;

-- ---------------- 账号（老板/复核人/财务/开单员 四角色） ----------------
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  username    VARCHAR(50)  NOT NULL COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  role        VARCHAR(20)  NOT NULL DEFAULT 'OPERATOR' COMMENT 'ADMIN 老板 / REVIEW 复核人 / FINANCE 财务 / OPERATOR 开单员',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 停用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='账号';

-- ---------------- 客户档案（蔗农 / 经纪人 / 合作社，欠款主体） ----------------
CREATE TABLE customer (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50)  NOT NULL COMMENT '姓名 / 名称',
  type        VARCHAR(20)  NOT NULL DEFAULT 'FARMER' COMMENT 'FARMER 蔗农 / BROKER 经纪人 / COOP 合作社',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  address     VARCHAR(100) DEFAULT NULL COMMENT '地址',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用（停用后不可被新单引用）',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_name (name)
) ENGINE=InnoDB COMMENT='客户档案';

-- ---------------- 甘蔗品种 ----------------
CREATE TABLE variety (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50)  NOT NULL COMMENT '品种名称',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='甘蔗品种';

-- ---------------- 价格政策（品种 × 单价 × 生效区间，状态按日期自动流转不落库） ----------------
CREATE TABLE price_policy (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  variety_id   BIGINT        NOT NULL COMMENT '品种 id',
  variety_name VARCHAR(50)   NOT NULL COMMENT '品种名（冗余快照）',
  price        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价（元/吨）',
  start_date   DATE          NOT NULL COMMENT '生效日期（含）',
  end_date     DATE          DEFAULT NULL COMMENT '失效日期（含），NULL 表示长期有效',
  remark       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  created_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_variety_date (variety_id, start_date)
) ENGINE=InnoDB COMMENT='价格政策';

-- ---------------- 收购单（核心表） ----------------
-- 计算规则：净重 = 毛重 − 皮重；扣杂量 = 净重 × 含杂率%；计价重量 = 净重 − 扣杂量；金额 = 计价重量 × 单价（四舍五入 2 位）
CREATE TABLE purchase_order (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  order_no        VARCHAR(32)   NOT NULL COMMENT '单号：yyyyMMdd + 4 位当日流水',
  order_date      DATE          NOT NULL COMMENT '收购日期（日结维度）',
  customer_id     BIGINT        NOT NULL COMMENT '客户 id',
  customer_name   VARCHAR(50)   NOT NULL COMMENT '客户名称（冗余快照）',
  customer_type   VARCHAR(20)   DEFAULT NULL COMMENT '客户类型（冗余快照）',
  variety_id      BIGINT        NOT NULL COMMENT '品种 id',
  variety_name    VARCHAR(50)   NOT NULL COMMENT '品种名（冗余快照）',
  plate_no        VARCHAR(20)   DEFAULT NULL COMMENT '车牌号',
  driver          VARCHAR(50)   DEFAULT NULL COMMENT '司机',
  gross_weight    DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '毛重（吨）',
  tare_weight     DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '皮重（吨）',
  net_weight      DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '净重 = 毛重 − 皮重（吨）',
  impurity_rate   DECIMAL(5,2)  NOT NULL DEFAULT 0 COMMENT '含杂率（%）',
  deduction_weight DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '扣杂量 = 净重 × 含杂率%（吨）',
  calc_weight     DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '计价重量 = 净重 − 扣杂量（吨）',
  price           DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '成交单价（元/吨）',
  policy_price    DECIMAL(10,2) DEFAULT NULL COMMENT '政策单价（开单时带出的原价，改价留痕）',
  price_changed_by VARCHAR(50)  DEFAULT NULL COMMENT '改价人（成交单价 ≠ 政策单价时记录）',
  amount          DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额 = 计价重量 × 单价（四舍五入 2 位）',
  remark          VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  status          VARCHAR(10)   NOT NULL DEFAULT 'PENDING' COMMENT 'DRAFT 草稿 / PENDING 待复核 / CONFIRMED 已确认 / VOIDED 已作废',
  reviewed_by     BIGINT        DEFAULT NULL COMMENT '复核人 id',
  reviewed_by_name VARCHAR(50)  DEFAULT NULL COMMENT '复核人姓名',
  reviewed_at     DATETIME      DEFAULT NULL COMMENT '复核时间',
  void_reason     VARCHAR(255)  DEFAULT NULL COMMENT '作废原因',
  voided_by       BIGINT        DEFAULT NULL COMMENT '作废人 id',
  voided_at       DATETIME      DEFAULT NULL COMMENT '作废时间',
  created_by      BIGINT        DEFAULT NULL COMMENT '开单人 id',
  created_by_name VARCHAR(50)   DEFAULT NULL COMMENT '开单人姓名',
  created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_date (order_date),
  KEY idx_customer (customer_id),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='收购单';

-- ---------------- 付款记录（登记后不可修改，只可作废） ----------------
CREATE TABLE payment (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  customer_id     BIGINT        NOT NULL COMMENT '客户 id',
  customer_name   VARCHAR(50)   NOT NULL COMMENT '客户名称（冗余快照）',
  amount          DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '付款金额（元）',
  method          VARCHAR(10)   NOT NULL DEFAULT 'CASH' COMMENT 'CASH 现金 / TRANSFER 转账 / OTHER 其他',
  handler         VARCHAR(50)   DEFAULT NULL COMMENT '经办人',
  pay_date        DATE          NOT NULL COMMENT '付款日期',
  voucher_url     VARCHAR(255)  DEFAULT NULL COMMENT '凭证照片（可选）',
  status          VARCHAR(10)   NOT NULL DEFAULT 'REGISTERED' COMMENT 'REGISTERED 已登记 / VOIDED 已作废（作废回滚余额）',
  void_reason     VARCHAR(255)  DEFAULT NULL COMMENT '作废原因',
  created_by      BIGINT        DEFAULT NULL COMMENT '登记人 id',
  created_by_name VARCHAR(50)   DEFAULT NULL COMMENT '登记人姓名',
  created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_customer_date (customer_id, pay_date)
) ENGINE=InnoDB COMMENT='付款记录';

-- ============================================================
-- 演示种子数据（全部为脱敏假名 / 假电话）
-- 账号密码均为 123456（BCrypt）
-- ============================================================
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('admin',  '$2a$10$IG1XFb5h6MmvaVkFG/nRFOLqXEoM91.FcsBzjbttZOW2Znlcw3TrK', '覃振华', '13800000001', 'ADMIN',    1),
('fuhe',   '$2a$10$IG1XFb5h6MmvaVkFG/nRFOLqXEoM91.FcsBzjbttZOW2Znlcw3TrK', '陆复核', '13800000002', 'REVIEW',   1),
('caiwu',  '$2a$10$IG1XFb5h6MmvaVkFG/nRFOLqXEoM91.FcsBzjbttZOW2Znlcw3TrK', '蓝财务', '13800000003', 'FINANCE',  1),
('kaidan', '$2a$10$IG1XFb5h6MmvaVkFG/nRFOLqXEoM91.FcsBzjbttZOW2Znlcw3TrK', '韦开单', '13800000004', 'OPERATOR', 1);

INSERT INTO customer (name, type, phone, address, remark, status) VALUES
('黄老三',          'FARMER', '13800000001', '崇左市江州区罗白乡那榜村',     '老蔗农，月结', 1),
('韦秀英',          'FARMER', '13800000002', '崇左市江州区濑湍镇',           NULL,           1),
('覃有福',          'FARMER', '13800000003', '崇左市扶绥县渠旧镇',           '偶尔送蔗',     1),
('蓝仔成',          'BROKER', '13800000004', '崇左市江南区糖料市场',         '经纪人，量大', 1),
('岜桑收蔗合作社',   'COOP',   '13800000005', '崇左市江州区江州镇岜桑村',     '合作社统一结算', 1),
('周木匠',          'FARMER', '13800000006', '崇左市江州区新和镇',           '已改行，停用', 0);

INSERT INTO variety (name, remark, status) VALUES
('桂糖42号',   '主力品种，高糖高产',   1),
('粤糖93-159', '老品种，含杂略高',     1),
('桂柳05-136', '新台糖替代品种',       1),
('云蔗08-1600', '早熟品种',            1);

-- 价格政策：3 条生效中 + 1 条未生效 + 1 条已失效（状态按日期自动流转，不落库）
INSERT INTO price_policy (variety_id, variety_name, price, start_date, end_date, remark) VALUES
(1, '桂糖42号',    540.00, '2026-09-01', '2026-12-31', '本榨季政策价'),
(2, '粤糖93-159',  525.00, '2026-09-01', '2026-12-31', '本榨季政策价'),
(3, '桂柳05-136',  515.00, '2026-09-01', '2026-11-30', '11 月底复调'),
(4, '云蔗08-1600', 555.00, '2026-10-10', '2026-12-31', '早熟加价'),
(1, '桂糖42号',    560.00, '2027-01-01', NULL,         '下榨季预调价（未生效）'),
(2, '粤糖93-159',  500.00, '2026-06-01', '2026-08-31', '上榨季收尾价（已失效）');

-- ---------------- 收购单：覆盖近 7 天（工作台趋势/日结演示） ----------------
-- 含 PENDING 待复核 2 笔、VOIDED 已作废 1 笔、改价留痕 1 笔
INSERT INTO purchase_order
  (order_no, order_date, customer_id, customer_name, customer_type,
   variety_id, variety_name, plate_no, driver,
   gross_weight, tare_weight, net_weight, impurity_rate, deduction_weight, calc_weight,
   price, policy_price, price_changed_by, amount,
   created_by, created_by_name, status,
   reviewed_by, reviewed_by_name, reviewed_at,
   void_reason, voided_by, voided_at, remark)
VALUES
  ('202610150001','2026-10-15',1,'黄老三','FARMER',1,'桂糖42号','桂F·A12345','黄老三',58.6,12.8,45.800,0,0.000,45.800,540,NULL,NULL,24732.00,4,'韦开单','CONFIRMED','2','陆复核','2026-10-15 18:30:00',NULL,NULL,NULL,NULL),
  ('202610150002','2026-10-15',4,'蓝仔成','BROKER',2,'粤糖93-159','桂F·B67890','蓝仔成',62.4,14.2,48.200,2,0.964,47.236,525,NULL,NULL,24798.90,4,'韦开单','CONFIRMED','2','陆复核','2026-10-15 18:30:00',NULL,NULL,NULL,NULL),
  ('202610160001','2026-10-16',2,'韦秀英','FARMER',1,'桂糖42号','桂F·C11223','陆德明',49.8,11.5,38.300,0,0.000,38.300,540,NULL,NULL,20682.00,1,'覃振华','CONFIRMED','2','陆复核','2026-10-16 18:30:00',NULL,NULL,NULL,NULL),
  ('202610160002','2026-10-16',5,'岜桑收蔗合作社','COOP',3,'桂柳05-136','桂F·D44556','农志富',71.2,16.4,54.800,1.5,0.822,53.978,515,NULL,NULL,27798.67,4,'韦开单','CONFIRMED','2','陆复核','2026-10-16 18:30:00',NULL,NULL,NULL,'车斗加高'),
  ('202610170001','2026-10-17',1,'黄老三','FARMER',4,'云蔗08-1600','桂F·A12345','黄老三',55.3,13.1,42.200,0,0.000,42.200,555,NULL,NULL,23421.00,4,'韦开单','CONFIRMED','2','陆复核','2026-10-17 18:30:00',NULL,NULL,NULL,NULL),
  ('202610170002','2026-10-17',3,'覃有福','FARMER',1,'桂糖42号','桂F·E77889','覃有福',46.7,12.0,34.700,2,0.694,34.006,540,NULL,NULL,18363.24,4,'韦开单','PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'明早复核'),
  ('202610180001','2026-10-18',5,'岜桑收蔗合作社','COOP',2,'粤糖93-159','桂F·D44556','农志富',68.9,15.6,53.300,1,0.533,52.767,525,NULL,NULL,27702.68,1,'覃振华','CONFIRMED','2','陆复核','2026-10-18 18:30:00',NULL,NULL,NULL,NULL),
  ('202610180002','2026-10-18',2,'韦秀英','FARMER',3,'桂柳05-136','桂F·C11223','陆德明',52.5,13.9,38.600,0,0.000,38.600,515,NULL,NULL,19879.00,4,'韦开单','CONFIRMED','2','陆复核','2026-10-18 18:30:00',NULL,NULL,NULL,NULL),
  ('202610190001','2026-10-19',4,'蓝仔成','BROKER',1,'桂糖42号','桂F·B67890','蓝仔成',60.1,14.7,45.400,2,0.908,44.492,560,540,'韦开单',24915.52,4,'韦开单','CONFIRMED','2','陆复核','2026-10-19 18:30:00',NULL,NULL,NULL,'政策价540改560'),
  ('202610190002','2026-10-19',1,'黄老三','FARMER',4,'云蔗08-1600','桂F·A12345','黄老三',47.9,11.2,36.700,0,0.000,36.700,555,NULL,NULL,20368.50,4,'韦开单','CONFIRMED','2','陆复核','2026-10-19 18:30:00',NULL,NULL,NULL,NULL),
  ('202610200001','2026-10-20',3,'覃有福','FARMER',2,'粤糖93-159','桂F·E77889','覃有福',59.4,13.3,46.100,1,0.461,45.639,525,NULL,NULL,23960.48,4,'韦开单','CONFIRMED','2','陆复核','2026-10-20 18:30:00',NULL,NULL,NULL,NULL),
  ('202610200002','2026-10-20',2,'韦秀英','FARMER',1,'桂糖42号','桂F·C11223','陆德明',51.0,12.6,38.400,0,0.000,38.400,540,NULL,NULL,20736.00,4,'韦开单','VOIDED','2','陆复核','2026-10-20 18:30:00','磅单重复录入，作废重开','2','2026-10-20 19:20:00','磅单重复开'),
  ('202610210001','2026-10-21',1,'黄老三','FARMER',3,'桂柳05-136','桂F·A12345','黄老三',64.8,15.0,49.800,0,0.000,49.800,515,NULL,NULL,25647.00,4,'韦开单','CONFIRMED','2','陆复核','2026-10-21 18:30:00',NULL,NULL,NULL,NULL),
  ('202610210002','2026-10-21',5,'岜桑收蔗合作社','COOP',4,'云蔗08-1600','桂F·D44556','农志富',73.6,17.2,56.400,1.5,0.846,55.554,555,NULL,NULL,30832.47,1,'覃振华','CONFIRMED','2','陆复核','2026-10-21 18:30:00',NULL,NULL,NULL,NULL),
  ('202610220001','2026-10-22',4,'蓝仔成','BROKER',2,'粤糖93-159','桂F·B67890','蓝仔成',57.2,13.8,43.400,2,0.868,42.532,525,NULL,NULL,22329.30,4,'韦开单','PENDING',NULL,NULL,NULL,NULL,NULL,NULL,NULL),
  ('202610220002','2026-10-22',1,'黄老三','FARMER',1,'桂糖42号','桂F·A12345','黄老三',50.4,12.1,38.300,0,0.000,38.300,540,NULL,NULL,20682.00,4,'韦开单','CONFIRMED','2','陆复核','2026-10-22 18:30:00',NULL,NULL,NULL,NULL);

-- ---------------- 付款记录：4 条有效 + 1 条已作废（演示回滚） ----------------
INSERT INTO payment
  (customer_id, customer_name, amount, method, handler, pay_date, voucher_url, status, void_reason, created_by, created_by_name)
VALUES
  (1, '黄老三',          60000.00, 'TRANSFER', '蓝财务', '2026-10-20', NULL, 'REGISTERED', NULL,       3, '蓝财务'),
  (2, '韦秀英',          40561.00, 'TRANSFER', '蓝财务', '2026-10-21', NULL, 'REGISTERED', NULL,       3, '蓝财务'),
  (4, '蓝仔成',          30000.00, 'CASH',     '蓝财务', '2026-10-21', NULL, 'REGISTERED', NULL,       3, '蓝财务'),
  (5, '岜桑收蔗合作社',   50000.00, 'TRANSFER', '蓝财务', '2026-10-19', NULL, 'REGISTERED', NULL,       3, '蓝财务'),
  (2, '韦秀英',           5000.00, 'CASH',     '蓝财务', '2026-10-18', NULL, 'VOIDED',     '重复登记', 3, '蓝财务');
