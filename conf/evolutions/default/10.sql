# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups
alter table user add profil_Image varchar(255);

create table image (
  id                        bigint not null,
  uri                       varchar(255),
  image                     BINARY(5000),
  constraint uq_image_uri unique (uri),
  constraint pk_image primary key (id))
;


create sequence image_seq;

# --- !Downs
alter table user delete profil_Image;

drop table if exists image;

drop sequence if exists image_seq;

