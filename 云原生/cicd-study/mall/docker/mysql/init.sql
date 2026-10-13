-- MySQL 容器首次初始化时执行（数据目录为空时才会执行）
-- mall_user 库和 mall 账号由环境变量 MYSQL_DATABASE / MYSQL_USER 自动创建，这里补充订单库并授权
CREATE DATABASE IF NOT EXISTS mall_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON mall_order.* TO 'mall'@'%';
FLUSH PRIVILEGES;
