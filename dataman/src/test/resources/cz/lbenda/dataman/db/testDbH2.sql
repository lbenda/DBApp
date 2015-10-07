create schema "test";

create table "test".table1 (
  id integer identity,
  col varchar(20),
  primary key(id)
);

create table "test".table2 (
  id varchar(20) not null,
  col varchar(20),
  table1_id integer,
  primary key(ID),
  foreign key (table1_id) references "test".table1(ID)
);

create table "test".table3 (
  id1 varchar(20) not null,
  id2 integer,
  col varchar(20),
  primary key(id1, id2)
);

insert into "test".table1 (col) values
  ('value1'),
  ('value2'),
  ('value3');
insert into "test".table2 (id, col, table1_id) values
  ('table2_id1', 'table2_value1', 1),
  ('table2_id2', 'table2_value2', 1),
  ('table2_id3', 'table2_value3', 2);
insert into "test".table3 (id1, id2, col) values
  ('table3_id1', 0, 'table3_value1'),
  ('table3_id2', 1, 'table3_value2'),
  ('table3_id1', 1, 'table3_value3'),
  ('table3_id2', 0, 'table3_value4');
