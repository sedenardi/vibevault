CREATE DATABASE  IF NOT EXISTS `vibevault` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `vibevault`;
-- MySQL dump 10.13  Distrib 5.5.9, for Win32 (x86)
--
-- Host: sanders-server    Database: vibevault
-- ------------------------------------------------------
-- Server version	5.1.54-1ubuntu4

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `showTbl`
--

DROP TABLE IF EXISTS `showTbl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `showTbl` (
  `showId` int(11) NOT NULL AUTO_INCREMENT,
  `identifier` varchar(100) NOT NULL,
  `artistId` int(11) NOT NULL,
  `title` varchar(100) NOT NULL,
  `date` varchar(50) DEFAULT NULL,
  `source` varchar(400) DEFAULT NULL,
  `rating` decimal(3,2) DEFAULT NULL,
  `dateCreated` datetime NOT NULL,
  PRIMARY KEY (`showId`),
  UNIQUE KEY `showId_UNIQUE` (`showId`),
  KEY `IX_identifier` (`identifier`)
) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;