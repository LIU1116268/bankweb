-- PRD 检查清单：新增投产流程状态字段
-- 执行前请确认表 prd_check_list 已存在

ALTER TABLE `prd_check_list`
    ADD COLUMN `STATUS` varchar(30) DEFAULT 'DRAFT' COMMENT '投产流程状态(DRAFT/SUBMITTED/UAT_PASSED/PROD_READY/ARCHIVED)' AFTER `PROD_ENV_CHECK`;

-- 已有数据默认视为「已提交」，便于继续走 UAT/投产流转（可按需改为 DRAFT）
UPDATE `prd_check_list` SET `STATUS` = 'SUBMITTED' WHERE `STATUS` IS NULL OR `STATUS` = '';
