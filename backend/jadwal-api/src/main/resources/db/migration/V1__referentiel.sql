-- Référentiel : établissements et utilisateurs

create table etablissement (
    id            bigserial primary key,
    nom           varchar(200) not null,
    code          varchar(50)  not null unique,
    ville         varchar(100),
    telephone     varchar(30),
    email         varchar(150),
    statut        varchar(20)  not null default 'ACTIF',
    date_creation timestamptz  not null default now()
);

create table utilisateur (
    id                bigserial primary key,
    email             varchar(150) not null unique,
    mot_de_passe_hash varchar(100) not null,
    nom_complet       varchar(150) not null,
    role              varchar(30)  not null,
    etablissement_id  bigint       null references etablissement(id),
    actif             boolean      not null default true,
    date_creation     timestamptz  not null default now()
);

create index idx_utilisateur_etablissement on utilisateur(etablissement_id);
