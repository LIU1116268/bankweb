-- --------------------------------------------------------
-- 项目名称: 银行投产管理系统 - 脱敏原型
-- 模块描述: 数据库初始化脚本 (DDL)
-- 作者信息: LLQ (https://github.com/LIU1116268)
-- 最后更新: 2026-03
-- --------------------------------------------------------

-- 下列表格信息均为随机产生，无真实信息关联
CREATE TABLE `sys_dept` (
                            `dept_id`    bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '部门id',
                            `parent_id`  bigint(20)   DEFAULT '0'              COMMENT '父部门id',
                            `dept_name`  varchar(30)  DEFAULT ''               COMMENT '部门名称',
                            `order_num`  int(4)       DEFAULT '0'              COMMENT '显示顺序',
                            `status`     char(1)      DEFAULT '0'              COMMENT '部门状态（0正常 1停用）',
                            `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
TRUNCATE TABLE `sys_dept`;
-- 1. 顶级节点：总行
INSERT INTO `sys_dept` VALUES (100, 0, '全国银行总行', 1, '0', NOW());

-- =============================================
-- 2. 第二层：4 个省份分行（挂在总行 100 下，共 4 条）
-- =============================================
INSERT INTO `sys_dept` VALUES (101, 100, '四川省分行', 1, '0', NOW());
INSERT INTO `sys_dept` VALUES (102, 100, '广东省分行', 2, '0', NOW());
INSERT INTO `sys_dept` VALUES (103, 100, '江苏省分行', 3, '0', NOW());
INSERT INTO `sys_dept` VALUES (104, 100, '浙江省分行', 4, '0', NOW());

-- =============================================
-- 3. 第三层：各省份下属支行（共 15 条）
-- 总计：1 + 4 + 15 = 20 条
-- =============================================

-- 四川分行下属支行（101）
INSERT INTO `sys_dept` VALUES (105, 101, '成都武侯支行', 1, '0', NOW());
INSERT INTO `sys_dept` VALUES (106, 101, '成都高新支行', 2, '0', NOW());
INSERT INTO `sys_dept` VALUES (107, 101, '绵阳涪城支行', 3, '0', NOW());
INSERT INTO `sys_dept` VALUES (108, 101, '德阳旌阳支行', 4, '0', NOW());
INSERT INTO `sys_dept` VALUES (109, 101, '宜宾翠屏支行', 5, '0', NOW());

-- 广东分行下属支行（102）
INSERT INTO `sys_dept` VALUES (110, 102, '广州天河支行', 1, '0', NOW());
INSERT INTO `sys_dept` VALUES (111, 102, '深圳南山支行', 2, '0', NOW());
INSERT INTO `sys_dept` VALUES (112, 102, '佛山禅城支行', 3, '0', NOW());
INSERT INTO `sys_dept` VALUES (113, 102, '东莞东城支行', 4, '0', NOW());

-- 江苏分行下属支行（103）
INSERT INTO `sys_dept` VALUES (114, 103, '南京玄武支行', 1, '0', NOW());
INSERT INTO `sys_dept` VALUES (115, 103, '苏州姑苏支行', 2, '0', NOW());
INSERT INTO `sys_dept` VALUES (116, 103, '无锡滨湖支行', 3, '0', NOW());

-- 浙江分行下属支行（104）
INSERT INTO `sys_dept` VALUES (117, 104, '杭州西湖支行', 1, '0', NOW());
INSERT INTO `sys_dept` VALUES (118, 104, '宁波鄞州支行', 2, '0', NOW());
INSERT INTO `sys_dept` VALUES (119, 104, '温州鹿城支行', 3, '0', NOW());