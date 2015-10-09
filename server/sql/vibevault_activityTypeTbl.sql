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
-- Table structure for table `activityTypeTbl`
--

DROP TABLE IF EXISTS `activityTypeTbl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activityTypeTbl` (
  `activityTypeId` int(11) NOT NULL,
  `activityTypeName` varchar(45) NOT NULL,
  `activityTypeDescription` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`activityTypeId`),
  UNIQUE KEY `activityTypeId_UNIQUE` (`activityTypeId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activityTypeTbl`
--

LOCK TABLES `activityTypeTbl` WRITE;
/*!40000 ALTER TABLE `activityTypeTbl` DISABLE KEYS */;
INSERT INTO `activityTypeTbl` VALUES (1,'createUser',NULL),(2,'createArtist',NULL),(3,'createShow',NULL),(4,'vote',NULL),(5,'getShows',NULL),(6,'getArtists',NULL),(7,'getShowsByArtist',NULL);
/*!40000 ALTER TABLE `activityTypeTbl` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-06-11 17:22:43
