CREATE TABLE IF NOT EXISTS `account` (
  `oid` BIGINT(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`oid`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS `user` (
  `oid` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `password` VARCHAR(255) NOT NULL,
  `username` VARCHAR(255) NOT NULL,
  `account_oid` BIGINT(20) NULL DEFAULT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `register_time` DATETIME NULL DEFAULT NULL,
  `token` VARCHAR(255) NULL DEFAULT NULL,
  `valid` BIT(1) NULL DEFAULT NULL,
  PRIMARY KEY (`oid`) ,
  UNIQUE INDEX `username` (`username` ASC) ,
  INDEX `IDX__USER__USERNAME` (`account_oid` ASC) ,
  CONSTRAINT `FK__USER__ACCOUNT__ACCOUNT_OID`
    FOREIGN KEY (`account_oid`)
    REFERENCES `account` (`oid`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS `role` (
  `oid` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`oid`) )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS `user_role` (
  `user_oid` BIGINT(20) NOT NULL,
  `role_oid` BIGINT(20) NOT NULL,
  INDEX `IDX__USER_OID` (`user_oid` ASC) ,
  INDEX `IDX__ROLE_OID` (`role_oid` ASC) ,
  CONSTRAINT `FK__USER_ROLE__ROLE__ROLE_OID`
    FOREIGN KEY (`role_oid`)
    REFERENCES `role` (`oid`),
  CONSTRAINT `FK__USER_ROLE__USER__USER_OID`
    FOREIGN KEY (`user_oid`)
    REFERENCES `user` (`oid`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

CREATE VIEW tomcat_user_role
  AS SELECT u.username, r.name AS rolename FROM user_role ur JOIN user u ON ur.user_oid = u.oid JOIN role r ON ur.role_oid = r.oid;