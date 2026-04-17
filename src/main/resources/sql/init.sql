-- 1. 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（唯一）',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT '1' COMMENT '账号状态：0-禁用，1-正常',
  `audit_status` TINYINT NOT NULL DEFAULT '0' COMMENT '审核状态（针对学生）：0-待审核，1-审核通过，2-审核拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- 2. 角色表
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（如：ADMIN、TEACHER、STUDENT）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- 初始化角色数据
INSERT INTO `role` (`role_name`, `role_code`, `description`) VALUES 
('管理员', 'ADMIN', '系统最高权限管理员'),
('教师', 'TEACHER', '负责课程与题目管理'),
('学生', 'STUDENT', '参与在线练习与考试');

-- 3. 用户角色关联表
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

-- 4. 权限表
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父权限ID',
  `sort_order` INT DEFAULT '0' COMMENT '排序序号',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `permission` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

-- 5. 班级表
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
  `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '班主任ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级表';

-- 6. 学生班级关联表
DROP TABLE IF EXISTS `student_class`;
CREATE TABLE `student_class` (
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`student_id`, `class_id`),
  KEY `idx_class_id` (`class_id`),
  CONSTRAINT `fk_sc_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sc_class` FOREIGN KEY (`class_id`) REFERENCES `class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生班级关联表';

-- 26. 操作日志表 
DROP TABLE IF EXISTS `log`; 
CREATE TABLE `log` ( 
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', 
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID（可为NULL）', 
  `operation` VARCHAR(100) NOT NULL COMMENT '操作描述', 
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块', 
  `params` TEXT COMMENT '请求参数', 
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址', 
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间', 
  PRIMARY KEY (`id`), 
  KEY `idx_user_id` (`user_id`), 
  KEY `idx_module` (`module`), 
  KEY `idx_create_time` (`create_time`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
