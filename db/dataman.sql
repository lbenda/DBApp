create schema DATAMAN;

drop table DATAMAN.FIELDS_TYPE;
create table DATAMAN.FIELDS_TYPE (
  ID int identity,
  CINT int,
  CBOOLEAN boolean,
  CTINYINT tinyint,
  CSMALLINT smallint,
  CBIGINT bigint,
  CDECIMAL decimal(10, 5),  
  CDOUBLE double,
  CREAL real,
  CTIME time, 
  CDATE date,
  CTIMESTAMP timestamp,
  CBINARY binary(100),
  COTHER other,
  CVARCHAR varchar(100),
  CVARCHAR_IGNORECASE varchar_ignorecase(100),
  CCHAR char(100),
  CBLOB blob(1024),
  CCLOB clob(1024),
  CUUID uuid,
  CARRAY array,
  CGEOMETRY geometry,
  primary key(ID)
);

insert into DATAMAN.FIELDS_TYPE (CINT) values(NULL);
insert into DATAMAN.FIELDS_TYPE (CINT, CBOOLEAN, CTINYINT, CSMALLINT, CBIGINT, CDECIMAL, CDOUBLE, CREAL)
values(1, true, 15, 1024, 9223372036854775807, 6.3, 1658.674987, 8.7);
insert into DATAMAN.FIELDS_TYPE (CTIME, CDATE, CTIMESTAMP)
values('15:13:23.698', '2015-05-16', '2015-05-06 15:13:23.698');
insert into DATAMAN.FIELDS_TYPE (CBINARY)
values (X'01020304050607080910111213141516171819200A0B0C0D0E0F1A1B1C1D1E1F');
insert into DATAMAN.FIELDS_TYPE (CVARCHAR, CVARCHAR_IGNORECASE, CCHAR)
values ('varchar', 'varchar_ignorecase', 'character');
insert into DATAMAN.FIELDS_TYPE (CBLOB) values ('5AAB3C8D6E0112356A8F9E');
insert into DATAMAN.FIELDS_TYPE (CCLOB) values ('Text v cclobu');
insert into DATAMAN.FIELDS_TYPE (CARRAY) values (('pole1', 'pole2'));