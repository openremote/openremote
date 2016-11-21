INSERT INTO role VALUES (1, 'service-admin');
INSERT INTO role VALUES (2, 'account-owner');

INSERT INTO account VALUES (1);
INSERT INTO user ( oid,
  password ,
  username,
  account_oid,
  email,
  register_time ,
  token ,
  valid) VALUES (1, md5('password{admin}'), 'admin', 1, 'support@openremote.org', now(), NULL, 1);
INSERT INTO user_role VALUES (1, 1);

INSERT INTO account VALUES (2);
INSERT INTO user ( oid,
  password ,
  username,
  account_oid,
  email,
  register_time ,
  token ,
  valid)  VALUES (2, md5('password{secret}'), 'openremote', 2, 'support@openremote.org', now(), NULL, 1);
INSERT INTO user_role VALUES (2, 2);