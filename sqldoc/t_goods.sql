/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80022
 Source Host           : localhost:3306
 Source Schema         : seckill

 Target Server Type    : MySQL
 Target Server Version : 80022
 File Encoding         : 65001

 Date: 01/06/2022 13:25:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_goods
-- ----------------------------
DROP TABLE IF EXISTS `t_goods`;
CREATE TABLE `t_goods` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `goods_name` varchar(16) COLLATE utf8_bin DEFAULT NULL COMMENT '商品名称',
  `goods_title` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT '商品标题',
  `goods_img` varchar(256) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL COMMENT '商品图片',
  `goods_detail` longtext COLLATE utf8_bin COMMENT '商品详情',
  `goods_price` decimal(10,2) DEFAULT '0.00' COMMENT '商品价格',
  `goods_stock` int DEFAULT '0' COMMENT '商品库存，-1表示没有限制',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='商品表';

-- ----------------------------
-- Records of t_goods
-- ----------------------------
BEGIN;
INSERT INTO `t_goods` VALUES (1, 'IPHONE 13 256GB', 'IPHONE 13 256GB', 'https://img10.360buyimg.com/n1/s450x450_jfs/t1/205287/32/2544/52863/61227485Eb6661c3c/ebb306404137050b.jpg', '苹果11 Apple iPhone11二手苹果手机 游戏手机 4G双卡双待 二手9成新 黑色【店长推荐】 64G 全网通', 7299.00, 100);
INSERT INTO `t_goods` VALUES (2, 'IPHONE 13 128GB', 'IPHONE 13 128GB', 'https://img10.360buyimg.com/n1/jfs/t1/105868/33/28298/43855/625e8f4bEce52899d/3956e5fb4e284a85.jpg', '苹果手机iPhoneXR改13通双卡双待改苹果13/13pro二手手机128G XR改13pro远峰蓝（推荐） 64GB', 5800.00, 100);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
