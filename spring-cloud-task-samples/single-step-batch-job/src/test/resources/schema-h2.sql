CREATE TABLE IF NOT EXISTS item
(
   item_name varchar(55)
);

CREATE TABLE IF NOT EXISTS item_sample
(
   item_name varchar(55)
);

INSERT INTO item_sample (item_name) VALUES ('foo');
INSERT INTO item_sample (item_name) VALUES ('bar');
INSERT INTO item_sample (item_name) VALUES ('baz');
INSERT INTO item_sample (item_name) VALUES ('boo');
INSERT INTO item_sample (item_name) VALUES ('qux');
INSERT INTO item_sample (item_name) VALUES ('Job');
