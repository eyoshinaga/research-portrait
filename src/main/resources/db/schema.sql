-- 科研人员画像分析系统 - MySQL 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS `research_portrait` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `research_portrait`;

-- 1. 院系/部门表 (Institution) - 校内学院、系、研究所、研究中心、重点实验室、教学部等
CREATE TABLE `institution` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL COMMENT '院系/部门名称',
    `type` VARCHAR(50) COMMENT '部门类型（学院/系/研究所/研究中心/重点实验室/教学部）',
    `country` VARCHAR(100) COMMENT '国家/地区',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 科研人员表 (Researcher)
CREATE TABLE `researcher` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '姓名',
    `age` INT COMMENT '年龄',
    `department` VARCHAR(255) COMMENT '所属院系',
    `title` VARCHAR(100) COMMENT '职称',
    `research_field` VARCHAR(255) COMMENT '核心研究方向',
    `discipline_category` VARCHAR(50) COMMENT '学科门类（工学/理学/人文社科等）',
    `institution_id` BIGINT COMMENT '隶属机构ID',
    `influence_index` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '学术影响力指数',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`institution_id`) REFERENCES `institution`(`id`)
);

-- 3. 学术论文表 (Paper)
CREATE TABLE `paper` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(500) NOT NULL COMMENT '论文标题',
    `journal` VARCHAR(255) COMMENT '期刊名称',
    `doi` VARCHAR(100) UNIQUE COMMENT 'DOI号',
    `pub_date` DATE COMMENT '发表日期',
    `cited_num` INT DEFAULT 0 COMMENT '被引次数',
    `subject_field` VARCHAR(255) COMMENT '学科领域',
    `impact_factor` DECIMAL(5, 2) COMMENT '影响因子',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 论文作者关联表 (Paper_Researcher)
CREATE TABLE `paper_researcher` (
    `paper_id` BIGINT,
    `researcher_id` BIGINT,
    `author_order` INT COMMENT '作者排序',
    PRIMARY KEY (`paper_id`, `researcher_id`),
    FOREIGN KEY (`paper_id`) REFERENCES `paper`(`id`),
    FOREIGN KEY (`researcher_id`) REFERENCES `researcher`(`id`)
);

-- 5. 专利表 (Patent)
CREATE TABLE `patent` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(500) NOT NULL COMMENT '专利名称',
    `patent_no` VARCHAR(100) UNIQUE COMMENT '专利号',
    `apply_date` DATE COMMENT '申请日期',
    `grant_date` DATE COMMENT '授权日期',
    `type` VARCHAR(50) COMMENT '专利类型（发明/实用新型/外观）',
    `status` VARCHAR(50) COMMENT '法律状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. 专利发明人关联表 (Patent_Researcher)
CREATE TABLE `patent_researcher` (
    `patent_id` BIGINT,
    `researcher_id` BIGINT,
    PRIMARY KEY (`patent_id`, `researcher_id`),
    FOREIGN KEY (`patent_id`) REFERENCES `patent`(`id`),
    FOREIGN KEY (`researcher_id`) REFERENCES `researcher`(`id`)
);

-- 7. 科研项目表 (Project)
CREATE TABLE `project` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(500) NOT NULL COMMENT '项目名称',
    `source` VARCHAR(255) COMMENT '项目来源（如：国家自然科学基金）',
    `level` VARCHAR(50) COMMENT '项目级别（国家级/省部级/横向）',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `total_funding` DECIMAL(15, 2) COMMENT '总经费（万元）',
    `status` VARCHAR(50) COMMENT '项目状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. 项目成员关联表 (Project_Researcher)
CREATE TABLE `project_researcher` (
    `project_id` BIGINT,
    `researcher_id` BIGINT,
    `role` VARCHAR(50) COMMENT '角色（负责人/参与人）',
    PRIMARY KEY (`project_id`, `researcher_id`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`id`),
    FOREIGN KEY (`researcher_id`) REFERENCES `researcher`(`id`)
);

-- 9. 权重配置表 (Weight_Config) - 用于动态权重算法
CREATE TABLE `weight_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `discipline` VARCHAR(50) COMMENT '学科门类',
    `paper_weight` DECIMAL(3, 2) DEFAULT 0.4 COMMENT '论文权重',
    `patent_weight` DECIMAL(3, 2) DEFAULT 0.3 COMMENT '专利权重',
    `project_weight` DECIMAL(3, 2) DEFAULT 0.3 COMMENT '项目权重',
    `decay_rate` DECIMAL(3, 2) DEFAULT 0.05 COMMENT '时间衰减系数 lambda'
);
