-- --------------------------------------------------------
-- 项目名称: 银行投产管理系统 - 脱敏原型
-- 模块描述: 数据库初始化脚本 (DDL)
-- 作者信息: LLQ (https://github.com/LIU1116268)
-- 最后更新: 2026-03
-- --------------------------------------------------------

-- 日志信息表
CREATE TABLE `sys_oper_log` (
                                `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
                                `TITLE` varchar(50) DEFAULT '' COMMENT '模块标题',
                                `BUSINESS_TYPE` varchar(20) DEFAULT '' COMMENT '业务类型（INSERT, UPDATE, DELETE, UPLOAD）',
                                `METHOD` varchar(100) DEFAULT '' COMMENT '方法名称',
                                `OPER_USER` varchar(50) DEFAULT '' COMMENT '操作人员',
                                `OPER_URL` varchar(255) DEFAULT '' COMMENT '请求URL',
                                `OPER_PARAM` text COMMENT '请求参数',
                                `JSON_RESULT` text COMMENT '返回结果',
                                `STATUS` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
                                `ERROR_MSG` text COMMENT '错误消息',
                                `OPER_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `sys_oper_log` (`TITLE`, `BUSINESS_TYPE`, `METHOD`, `OPER_USER`, `OPER_URL`, `STATUS`)
VALUES ('系统测试', 'INIT', 'Test.init()', 'ADMIN', '/test', 0);