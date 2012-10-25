--
-- Copyright (C) 1998-2011 enStratusNetworks LLC
--
-- ====================================================================
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- ====================================================================
--

CREATE TABLE sequencer (
  name varchar(20) NOT NULL default '',
  next_key bigint(20) unsigned NOT NULL default '0',
  last_update bigint(20) unsigned NOT NULL default '0',
  spacing bigint(20) unsigned NOT NULL default '0',
  PRIMARY KEY  (name),
  UNIQUE KEY name (name,last_update),
  KEY name_2 (name,next_key,last_update)
) TYPE=InnoDB CHARSET=utf8;

CREATE TABLE `dsn_translation` (
  `owner_class` varchar(100) default NULL,
  `owner_id` varchar(100) NOT NULL default '',
  `attribute` varchar(100) NOT NULL default '',
  `language` varchar(2) NOT NULL default '',
  `country` varchar(2) default NULL,
  `translation` text,
  UNIQUE KEY `xlat_idx` (`owner_class`,`owner_id`,`attribute`,`language`,`country`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
