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
-- Temporary table structure for view `getArtistAllTimeVw`
--

DROP TABLE IF EXISTS `getArtistAllTimeVw`;
/*!50001 DROP VIEW IF EXISTS `getArtistAllTimeVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getArtistAllTimeVw` (
  `artistId` int(11),
  `artist` varchar(100),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getArtistAverageRatingVw`
--

DROP TABLE IF EXISTS `getArtistAverageRatingVw`;
/*!50001 DROP VIEW IF EXISTS `getArtistAverageRatingVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getArtistAverageRatingVw` (
  `artistId` int(11),
  `average` decimal(3,2)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getArtistDailyVw`
--

DROP TABLE IF EXISTS `getArtistDailyVw`;
/*!50001 DROP VIEW IF EXISTS `getArtistDailyVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getArtistDailyVw` (
  `artistId` int(11),
  `artist` varchar(100),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getArtistNewestAddedVw`
--

DROP TABLE IF EXISTS `getArtistNewestAddedVw`;
/*!50001 DROP VIEW IF EXISTS `getArtistNewestAddedVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getArtistNewestAddedVw` (
  `artistId` int(11),
  `artist` varchar(100),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getArtistNewestVotedVw`
--

DROP TABLE IF EXISTS `getArtistNewestVotedVw`;
/*!50001 DROP VIEW IF EXISTS `getArtistNewestVotedVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getArtistNewestVotedVw` (
  `artistId` int(11),
  `artist` varchar(100),
  `rating` decimal(3,2),
  `votes` decimal(25,0),
  `lastVote` datetime
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getArtistWeeklyVw`
--

DROP TABLE IF EXISTS `getArtistWeeklyVw`;
/*!50001 DROP VIEW IF EXISTS `getArtistWeeklyVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getArtistWeeklyVw` (
  `artistId` int(11),
  `artist` varchar(100),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getShowAllTimeVw`
--

DROP TABLE IF EXISTS `getShowAllTimeVw`;
/*!50001 DROP VIEW IF EXISTS `getShowAllTimeVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getShowAllTimeVw` (
  `identifier` varchar(100),
  `artist` varchar(100),
  `artistId` int(11),
  `title` varchar(100),
  `date` varchar(50),
  `source` varchar(400),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getShowDailyVw`
--

DROP TABLE IF EXISTS `getShowDailyVw`;
/*!50001 DROP VIEW IF EXISTS `getShowDailyVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getShowDailyVw` (
  `identifier` varchar(100),
  `artist` varchar(100),
  `artistId` int(11),
  `title` varchar(100),
  `date` varchar(50),
  `source` varchar(400),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getShowNewestAddedVw`
--

DROP TABLE IF EXISTS `getShowNewestAddedVw`;
/*!50001 DROP VIEW IF EXISTS `getShowNewestAddedVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getShowNewestAddedVw` (
  `identifier` varchar(100),
  `artist` varchar(100),
  `artistId` int(11),
  `title` varchar(100),
  `date` varchar(50),
  `source` varchar(400),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getShowNewestVotedVw`
--

DROP TABLE IF EXISTS `getShowNewestVotedVw`;
/*!50001 DROP VIEW IF EXISTS `getShowNewestVotedVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getShowNewestVotedVw` (
  `identifier` varchar(100),
  `artist` varchar(100),
  `artistId` int(11),
  `title` varchar(100),
  `date` varchar(50),
  `source` varchar(400),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `getShowWeeklyVw`
--

DROP TABLE IF EXISTS `getShowWeeklyVw`;
/*!50001 DROP VIEW IF EXISTS `getShowWeeklyVw`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `getShowWeeklyVw` (
  `identifier` varchar(100),
  `artist` varchar(100),
  `artistId` int(11),
  `title` varchar(100),
  `date` varchar(50),
  `source` varchar(400),
  `rating` decimal(3,2),
  `votes` decimal(25,0)
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `getArtistAllTimeVw`
--

/*!50001 DROP TABLE IF EXISTS `getArtistAllTimeVw`*/;
/*!50001 DROP VIEW IF EXISTS `getArtistAllTimeVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getArtistAllTimeVw` AS select `a`.`artistId` AS `artistId`,`a`.`artist` AS `artist`,`aver`.`average` AS `rating`,sum(`v`.`value`) AS `votes`,(case when (hour(timediff(now(),max(`v`.`dateCreated`))) = 0) then 'Less than an hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) = 1) then '1 hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) < 24) then concat(hour(timediff(now(),max(`v`.`dateCreated`))),' hours ago') when (hour(timediff(now(),max(`v`.`dateCreated`))) < 48) then '1 day ago' else concat(floor((hour(timediff(now(),max(`v`.`dateCreated`))) / 24)),' days ago') end) AS `lastVote` from (((`voteTbl` `v` join `showTbl` `s` on((`s`.`showId` = `v`.`showId`))) join `artistTbl` `a` on((`a`.`artistId` = `s`.`artistId`))) join `getArtistAverageRatingVw` `aver` on((`aver`.`artistId` = `a`.`artistId`))) group by `a`.`artistId`,`a`.`artist` order by sum(`v`.`value`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getArtistAverageRatingVw`
--

/*!50001 DROP TABLE IF EXISTS `getArtistAverageRatingVw`*/;
/*!50001 DROP VIEW IF EXISTS `getArtistAverageRatingVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getArtistAverageRatingVw` AS select `a1`.`artistId` AS `artistId`,cast(avg(`s1`.`rating`) as decimal(3,2)) AS `average` from (`artistTbl` `a1` join `showTbl` `s1` on((`s1`.`artistId` = `a1`.`artistId`))) where (`s1`.`rating` <> 0.00) group by `a1`.`artistId` union select `a2`.`artistId` AS `artistId`,cast('0.00' as decimal(3,2)) AS `average` from (`artistTbl` `a2` join `showTbl` `s2` on((`s2`.`artistId` = `a2`.`artistId`))) where (not(exists(select 1 from `showTbl` `s3` where ((`s3`.`artistId` = `a2`.`artistId`) and (`s3`.`rating` <> 0.00))))) group by `a2`.`artistId` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getArtistDailyVw`
--

/*!50001 DROP TABLE IF EXISTS `getArtistDailyVw`*/;
/*!50001 DROP VIEW IF EXISTS `getArtistDailyVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getArtistDailyVw` AS select `a`.`artistId` AS `artistId`,`a`.`artist` AS `artist`,`aver`.`average` AS `rating`,sum(`v`.`value`) AS `votes`,(case when (hour(timediff(now(),max(`v`.`dateCreated`))) = 0) then 'Less than an hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) = 1) then '1 hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) < 24) then concat(hour(timediff(now(),max(`v`.`dateCreated`))),' hours ago') when (hour(timediff(now(),max(`v`.`dateCreated`))) < 48) then '1 day ago' else concat(floor((hour(timediff(now(),max(`v`.`dateCreated`))) / 24)),' days ago') end) AS `lastVote` from (((`voteTbl` `v` join `showTbl` `s` on((`s`.`showId` = `v`.`showId`))) join `artistTbl` `a` on((`a`.`artistId` = `s`.`artistId`))) join `getArtistAverageRatingVw` `aver` on((`aver`.`artistId` = `a`.`artistId`))) where (`v`.`dateCreated` > (now() - interval 1 day)) group by `a`.`artistId`,`a`.`artist` order by sum(`v`.`value`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getArtistNewestAddedVw`
--

/*!50001 DROP TABLE IF EXISTS `getArtistNewestAddedVw`*/;
/*!50001 DROP VIEW IF EXISTS `getArtistNewestAddedVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getArtistNewestAddedVw` AS select `a`.`artistId` AS `artistId`,`a`.`artist` AS `artist`,`aver`.`average` AS `rating`,sum(`v`.`value`) AS `votes` from,(case when (hour(timediff(now(),max(`v`.`dateCreated`))) = 0) then 'Less than an hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) = 1) then '1 hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) < 24) then concat(hour(timediff(now(),max(`v`.`dateCreated`))),' hours ago') when (hour(timediff(now(),max(`v`.`dateCreated`))) < 48) then '1 day ago' else concat(floor((hour(timediff(now(),max(`v`.`dateCreated`))) / 24)),' days ago') end) AS `lastVote` (((`voteTbl` `v` join `showTbl` `s` on((`s`.`showId` = `v`.`showId`))) join `artistTbl` `a` on((`a`.`artistId` = `s`.`artistId`))) join `getArtistAverageRatingVw` `aver` on((`aver`.`artistId` = `a`.`artistId`))) group by `a`.`artistId`,`a`.`artist` order by `a`.`dateCreated` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getArtistNewestVotedVw`
--

/*!50001 DROP TABLE IF EXISTS `getArtistNewestVotedVw`*/;
/*!50001 DROP VIEW IF EXISTS `getArtistNewestVotedVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getArtistNewestVotedVw` AS select `a`.`artistId` AS `artistId`,`a`.`artist` AS `artist`,`aver`.`average` AS `rating`,sum(`v`.`value`) AS `votes`,(case when (hour(timediff(now(),max(`v`.`dateCreated`))) = 0) then 'Less than an hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) = 1) then '1 hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) < 24) then concat(hour(timediff(now(),max(`v`.`dateCreated`))),' hours ago') when (hour(timediff(now(),max(`v`.`dateCreated`))) < 48) then '1 day ago' else concat(floor((hour(timediff(now(),max(`v`.`dateCreated`))) / 24)),' days ago') end) AS `lastVote` from (((`voteTbl` `v` join `showTbl` `s` on((`s`.`showId` = `v`.`showId`))) join `artistTbl` `a` on((`a`.`artistId` = `s`.`artistId`))) join `getArtistAverageRatingVw` `aver` on((`aver`.`artistId` = `a`.`artistId`))) group by `a`.`artistId`,`a`.`artist` order by max(`v`.`dateCreated`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getArtistWeeklyVw`
--

/*!50001 DROP TABLE IF EXISTS `getArtistWeeklyVw`*/;
/*!50001 DROP VIEW IF EXISTS `getArtistWeeklyVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getArtistWeeklyVw` AS select `a`.`artistId` AS `artistId`,`a`.`artist` AS `artist`,`aver`.`average` AS `rating`,sum(`v`.`value`) AS `votes`,(case when (hour(timediff(now(),max(`v`.`dateCreated`))) = 0) then 'Less than an hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) = 1) then '1 hour ago' when (hour(timediff(now(),max(`v`.`dateCreated`))) < 24) then concat(hour(timediff(now(),max(`v`.`dateCreated`))),' hours ago') when (hour(timediff(now(),max(`v`.`dateCreated`))) < 48) then '1 day ago' else concat(floor((hour(timediff(now(),max(`v`.`dateCreated`))) / 24)),' days ago') end) AS `lastVote` from (((`voteTbl` `v` join `showTbl` `s` on((`s`.`showId` = `v`.`showId`))) join `artistTbl` `a` on((`a`.`artistId` = `s`.`artistId`))) join `getArtistAverageRatingVw` `aver` on((`aver`.`artistId` = `a`.`artistId`))) where (`v`.`dateCreated` > (now() - interval 7 day)) group by `a`.`artistId`,`a`.`artist` order by sum(`v`.`value`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getShowAllTimeVw`
--

/*!50001 DROP TABLE IF EXISTS `getShowAllTimeVw`*/;
/*!50001 DROP VIEW IF EXISTS `getShowAllTimeVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getShowAllTimeVw` AS select distinct `s`.`identifier` AS `identifier`,`a`.`artist` AS `artist`,`a`.`artistId` AS `artistId`,`s`.`title` AS `title`,`s`.`date` AS `date`,`s`.`source` AS `source`,`s`.`rating` AS `rating`,sum(`v`.`value`) AS `votes` from ((`voteTbl` `v` join `showTbl` `s` on((`v`.`showId` = `s`.`showId`))) join `artistTbl` `a` on((`s`.`artistId` = `a`.`artistId`))) group by `v`.`showId` order by sum(`v`.`value`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getShowDailyVw`
--

/*!50001 DROP TABLE IF EXISTS `getShowDailyVw`*/;
/*!50001 DROP VIEW IF EXISTS `getShowDailyVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getShowDailyVw` AS select distinct `s`.`identifier` AS `identifier`,`a`.`artist` AS `artist`,`a`.`artistId` AS `artistId`,`s`.`title` AS `title`,`s`.`date` AS `date`,`s`.`source` AS `source`,`s`.`rating` AS `rating`,sum(`v`.`value`) AS `votes` from ((`voteTbl` `v` join `showTbl` `s` on((`v`.`showId` = `s`.`showId`))) join `artistTbl` `a` on((`s`.`artistId` = `a`.`artistId`))) where (`v`.`dateCreated` > (now() - interval 1 day)) group by `v`.`showId` order by sum(`v`.`value`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getShowNewestAddedVw`
--

/*!50001 DROP TABLE IF EXISTS `getShowNewestAddedVw`*/;
/*!50001 DROP VIEW IF EXISTS `getShowNewestAddedVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getShowNewestAddedVw` AS select distinct `s`.`identifier` AS `identifier`,`a`.`artist` AS `artist`,`a`.`artistId` AS `artistId`,`s`.`title` AS `title`,`s`.`date` AS `date`,`s`.`source` AS `source`,`s`.`rating` AS `rating`,sum(`v`.`value`) AS `votes` from ((`voteTbl` `v` join `showTbl` `s` on((`v`.`showId` = `s`.`showId`))) join `artistTbl` `a` on((`s`.`artistId` = `a`.`artistId`))) group by `v`.`showId` order by `s`.`dateCreated` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getShowNewestVotedVw`
--

/*!50001 DROP TABLE IF EXISTS `getShowNewestVotedVw`*/;
/*!50001 DROP VIEW IF EXISTS `getShowNewestVotedVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getShowNewestVotedVw` AS select distinct `s`.`identifier` AS `identifier`,`a`.`artist` AS `artist`,`a`.`artistId` AS `artistId`,`s`.`title` AS `title`,`s`.`date` AS `date`,`s`.`source` AS `source`,`s`.`rating` AS `rating`,sum(`v`.`value`) AS `votes` from ((`voteTbl` `v` join `showTbl` `s` on((`v`.`showId` = `s`.`showId`))) join `artistTbl` `a` on((`s`.`artistId` = `a`.`artistId`))) group by `v`.`showId` order by `v`.`dateCreated` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `getShowWeeklyVw`
--

/*!50001 DROP TABLE IF EXISTS `getShowWeeklyVw`*/;
/*!50001 DROP VIEW IF EXISTS `getShowWeeklyVw`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`sanders`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `getShowWeeklyVw` AS select distinct `s`.`identifier` AS `identifier`,`a`.`artist` AS `artist`,`a`.`artistId` AS `artistId`,`s`.`title` AS `title`,`s`.`date` AS `date`,`s`.`source` AS `source`,`s`.`rating` AS `rating`,sum(`v`.`value`) AS `votes` from ((`voteTbl` `v` join `showTbl` `s` on((`v`.`showId` = `s`.`showId`))) join `artistTbl` `a` on((`s`.`artistId` = `a`.`artistId`))) where (`v`.`dateCreated` > (now() - interval 7 day)) group by `v`.`showId` order by sum(`v`.`value`) desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Dumping routines for database 'vibevault'
--
/*!50003 DROP PROCEDURE IF EXISTS `getArtistsPrc` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`sanders`@`%`*/ /*!50003 PROCEDURE `getArtistsPrc`(
    userId      int,
    resultType  int,
    numResults  int,
    offset      int
)
BEGIN

set @rightNow = NOW();
set @userPK = userId;
set @lim = numResults;
Set @off = offset;

if (Select 1 from userTbl u where u.userId = @userPK) is null
    OR (@userPK = 0) Then
    Insert into userTbl(dateCreated)
    Select @rightNow;
    set @userPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,1,@rightNow,'Created during getShows';
End If;


Insert into activityTbl(userId,activityTypeId,resultTypeId,numResults,activityDate,activityNotes)
Select @userPK,6,resultType,@lim,@rightNow,concat('Returning from offset: ',cast(@off as char));

if resultType = 1 then
Prepare STMT from 'Select *,? as userId from getArtistAllTimeVw LIMIT ?,?';
elseif resultType = 2 then
Prepare STMT from 'Select *,? as userId from getArtistDailyVw LIMIT ?,?';
elseif resultType = 3 then
Prepare STMT from 'Select *,? as userId from getArtistWeeklyVw LIMIT ?,?';
elseif resultType = 4 then
Prepare STMT from 'Select *,? as userId from getArtistNewestAddedVw LIMIT ?,?';
elseif resultType = 5 then
Prepare STMT from 'Select *,? as userId from getArtistNewestVotedVw LIMIT ?,?';
else
Prepare STMT from 'Select *,? as userId from getArtistAllTimeVw LIMIT ?,?';
end if;

EXECUTE STMT using @userPK,@off,@lim;

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `getShowsByArtistPrc` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`sanders`@`%`*/ /*!50003 PROCEDURE `getShowsByArtistPrc`(
    userId      int,
    resultType  int,
    numResults  int,
    offset      int,
    artistId    int
)
BEGIN

set @rightNow = NOW();
set @userPK = userId;
set @lim = numResults;
Set @off = offset;
Set @artistPK = artistId;

if (Select 1 from userTbl u where u.userId = @userPK) is null
    OR (@userPK = 0) Then
    Insert into userTbl(dateCreated)
    Select @rightNow;
    set @userPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,1,@rightNow,'Created during getShows';
End If;


Insert into activityTbl(userId,activityTypeId,resultTypeId,numResults,activityDate,activityNotes)
Select @userPK,7,resultType,@lim,@rightNow,
concat(concat(concat('Returning artist: ',cast(@artistPK as char)),'from offset: '),cast(@off as char));

if resultType = 1 then
Prepare STMT from 'Select *,? as userId from getShowAllTimeVw where artistId = ? LIMIT ?,?';
elseif resultType = 2 then
Prepare STMT from 'Select *,? as userId from getShowDailyVw where artistId = ? LIMIT ?,?';
elseif resultType = 3 then
Prepare STMT from 'Select *,? as userId from getShowWeeklyVw where artistId = ? LIMIT ?,?';
elseif resultType = 4 then
Prepare STMT from 'Select *,? as userId from getShowNewestAddedVw where artistId = ? LIMIT ?,?';
elseif resultType = 5 then
Prepare STMT from 'Select *,? as userId from getShowNewestVotedVw where artistId = ? LIMIT ?,?';
else
Prepare STMT from 'Select *,? as userId from getShowAllTimeVw where artistId = ? LIMIT ?,?';
end if;

EXECUTE STMT using @userPK,@artistPK,@off,@lim;

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `getShowsPrc` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`sanders`@`%`*/ /*!50003 PROCEDURE `getShowsPrc`(
    userId      int,
    resultType  int,
    numResults  int,
    offset      int
)
BEGIN

set @rightNow = NOW();
set @userPK = userId;
set @lim = numResults;
Set @off = offset;

if (Select 1 from userTbl u where u.userId = @userPK) is null
    OR (@userPK = 0) Then
    Insert into userTbl(dateCreated)
    Select @rightNow;
    set @userPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,1,@rightNow,'Created during getShows';
End If;


Insert into activityTbl(userId,activityTypeId,resultTypeId,numResults,activityDate,activityNotes)
Select @userPK,5,resultType,@lim,@rightNow,concat('Returning from offset: ',cast(@off as char));

if resultType = 1 then
Prepare STMT from 'Select *,? as userId from getShowAllTimeVw LIMIT ?,?';
elseif resultType = 2 then
Prepare STMT from 'Select *,? as userId from getShowDailyVw LIMIT ?,?';
elseif resultType = 3 then
Prepare STMT from 'Select *,? as userId from getShowWeeklyVw LIMIT ?,?';
elseif resultType = 4 then
Prepare STMT from 'Select *,? as userId from getShowNewestAddedVw LIMIT ?,?';
elseif resultType = 5 then
Prepare STMT from 'Select *,? as userId from getShowNewestVotedVw LIMIT ?,?';
else
Prepare STMT from 'Select *,? as userId from getShowAllTimeVw LIMIT ?,?';
end if;

EXECUTE STMT using @userPK,@off,@lim;

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `votePrc` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50020 DEFINER=`sanders`@`%`*/ /*!50003 PROCEDURE `votePrc`(
    id              int,
    showIdentifier  varchar(100),
    showArtist      varchar(100),
    showTitle       varchar(100),
    showDate        varchar(50),
    showSource      varchar(400),
    showRating      varchar(5)
)
BEGIN

Set @rightNow = NOW();
Set @resultText = 'Vote failed';
set @userPK = id;
set @artistPK = 0;
set @showPK = 0;

if (Select 1 from userTbl where userId = @userPK) is null
    OR (@userPK = 0) Then
    Insert into userTbl(dateCreated)
    Select @rightNow;
    Set @userPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,1,@rightNow,'Created during vote';
End If;

if (Select 1 from artistTbl where artist like showArtist) is null Then
    Insert into artistTbl(artist,dateCreated)
    Select showArtist,@rightNow;
    Set @artistPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,2,@rightNow,concat('Created artist id: ',cast(@artistPK as char));
else
    Select artistId into @artistPK
    from artistTbl where artist like showArtist;
End If;


if (Select 1 from showTbl where identifier like showIdentifier) is null Then
    Insert into showTbl(identifier,artistId,title,date,source,rating,dateCreated)
    Select showIdentifier,@artistPK,showTitle,showDate,showSource,showRating,@rightNow;
    Set @showPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,3,@rightNow,concat('Created show id: ',cast(@showPK as char));
else
    Select showId into @showPK
    from showTbl where identifier like showIdentifier;
    Update showTbl set rating = showRating where showId = @showPK;
End If;


if (Select 1 from voteTbl where userId = @userPK and showId = @showPK 
    and DATE(dateCreated) > DATE(SUBDATE(@rightNow,1))) is null Then
    Insert into voteTbl(userId,showId,dateCreated)
    Select @userPK,@showPK,@rightNow;
    Set @resultText = 'Vote successful';
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,4,@rightNow,concat('Vote successful for show id: ',cast(@showPK as char));
else
    Select 'Only one vote per show per day' into @resultText;
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,4,@rightNow,concat('Vote not successful for show id: ',cast(@showPK as char));
End If;

Select @userPK as 'userId', @resultText as 'resultText';

END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-06-11 17:23:23
