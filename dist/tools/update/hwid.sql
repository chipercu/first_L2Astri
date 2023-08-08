CREATE TABLE IF NOT EXISTS `hwid_log` (
  `server_id` INT(10) NOT NULL,
  `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `account` VARCHAR(16) NOT NULL,
  `ip` VARCHAR(16) NOT NULL,
  `hwid` VARCHAR(32) NOT NULL,
  PRIMARY KEY  (`server_id`,`time`,`account`),
  KEY `account` (`account`),
  KEY `ip` (`ip`),
  KEY `hwid` (`hwid`)
) ENGINE=MyISAM;