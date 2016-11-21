CREATE TABLE IF NOT EXISTS discovered_device (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  device_identifier VARCHAR(255) NOT NULL,
  device_name VARCHAR(255) NOT NULL,
  protocol VARCHAR(255),
  model VARCHAR(255),
  type VARCHAR(255),
  account_id VARCHAR(64),
  PRIMARY KEY (id))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS discovered_device_attributes (
  discovered_device_id BIGINT(20) NOT NULL,
  attribute_key VARCHAR(255) NOT NULL,
  attribute_value VARCHAR(1000) NOT NULL,
  PRIMARY KEY (discovered_device_id, attribute_key),
  CONSTRAINT FOREIGN KEY (discovered_device_id)
  REFERENCES discovered_device (id))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;