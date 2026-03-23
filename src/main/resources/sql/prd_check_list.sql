-- --------------------------------------------------------
-- 项目名称: 银行投产管理系统 - 脱敏原型
-- 模块描述: 数据库初始化脚本 (DDL)
-- 作者信息: LLQ (https://github.com/LIU1116268)
-- 最后更新: 2026-03
-- --------------------------------------------------------

-- 1、创建PRD检查清单表（未分配部门）
CREATE TABLE `prd_check_list` (
                                  `ID` varchar(50) NOT NULL COMMENT 'ID',
                                  `WINDOW_VER_ID` varchar(200) NOT NULL COMMENT '投产版本号',
                                  `DEMAND_NAME` varchar(500) DEFAULT NULL COMMENT '需求模块',
                                  `PROD_CONTENT` varchar(500) DEFAULT NULL COMMENT '投产内容',
                                  `RELA_FEATURE` varchar(500) DEFAULT NULL COMMENT '关联分支',
                                  `RELA_SCRIPT` varchar(500) DEFAULT NULL COMMENT '关联脚本',
                                  `PROD_TYPE` varchar(10) DEFAULT NULL COMMENT '变更类型(新增/修改)',
                                  `DEMAND_MANAGER` varchar(200) DEFAULT NULL COMMENT '需求负责人',
                                  `TECH_MANAGER` varchar(200) DEFAULT NULL COMMENT '开发负责人',
                                  `UAT_ENV_CHECK` varchar(20) DEFAULT NULL COMMENT 'UAT测试(已验证/未通过)',
                                  `PROD_ENV_CHECK` varchar(20) DEFAULT NULL COMMENT '生产测试(已验证/未通过)',
                                  `REMARK` varchar(400) DEFAULT NULL COMMENT '备注信息',
                                  `ATTACHMENT_PATH` varchar(500) DEFAULT NULL COMMENT '附件地址',
                                  `CREATE_USER` varchar(50) DEFAULT NULL COMMENT '创建人',
                                  `CREATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `UPDATE_USER` varchar(50) DEFAULT NULL COMMENT '更新人',
                                  `UPDATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PRD检查清单表';

-- 2、插入部分数据，可自行随机生成
-- 第一条：新增操作记录
INSERT INTO `prd_check_list` (
    `ID`, `WINDOW_VER_ID`, `DEMAND_NAME`, `PROD_CONTENT`, `RELA_FEATURE`,
    `RELA_SCRIPT`, `PROD_TYPE`, `DEMAND_MANAGER`, `TECH_MANAGER`,
    `UAT_ENV_CHECK`, `PROD_ENV_CHECK`, `CREATE_USER`
) VALUES (
             '202603180001', 'VER_2026_01', '用户登录模块优化', '增加第三方快捷登录接口', 'feature/login-oauth',
             'v1.0_init_db.sql', '新增', '张三', '李四',
             '已验证', '未通过', 'admin'
         );

-- 第二条：修改操作记录
INSERT INTO `prd_check_list` (
    `ID`, `WINDOW_VER_ID`, `DEMAND_NAME`, `PROD_CONTENT`, `RELA_FEATURE`,
    `RELA_SCRIPT`, `PROD_TYPE`, `DEMAND_MANAGER`, `TECH_MANAGER`,
    `UAT_ENV_CHECK`, `REMARK`, `CREATE_USER`
) VALUES (
             '202603180002', 'VER_2026_02', '支付结算中心', '修复已知汇率计算Bug', 'fix/rate-calc',
             'N/A', '修改', '王五', '赵六',
             '已验证', '紧急修复，无需生产预验证', 'system'
         );

-- 3、为表新增一列部门信息
UPDATE `prd_check_list` SET `DEPT_ID` = NULL;

-- 4、创建好sys_dept这个部门归属表后，再执行下面，为每条记录随机分配部门信息
UPDATE prd_check_list p
SET p.DEPT_ID = (
    SELECT dept_id
    FROM sys_dept
    ORDER BY RAND()
    LIMIT 1
);