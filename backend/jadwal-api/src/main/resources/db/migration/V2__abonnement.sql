-- Abonnement : plans, abonnements et paiements manuels

create table plan_abonnement (
    id          bigserial primary key,
    code        varchar(50)   not null unique,
    nom         varchar(100)  not null,
    prix_annuel numeric(10,2) not null,
    description text,
    actif       boolean       not null default true
);

create table abonnement (
    id               bigserial primary key,
    etablissement_id bigint      not null references etablissement(id),
    plan_id          bigint      not null references plan_abonnement(id),
    date_debut       date        not null,
    date_fin         date        not null,
    statut           varchar(30) not null default 'EN_ATTENTE_PAIEMENT',
    date_creation    timestamptz not null default now()
);

create table paiement (
    id             bigserial primary key,
    abonnement_id  bigint        not null references abonnement(id),
    montant        numeric(10,2) not null,
    mode           varchar(20)   not null,
    reference      varchar(100),
    date_paiement  date          not null,
    statut         varchar(20)   not null default 'CONFIRME',
    note           text,
    enregistre_par bigint        null references utilisateur(id),
    date_creation  timestamptz   not null default now()
);

create index idx_abonnement_etablissement on abonnement(etablissement_id);
create index idx_paiement_abonnement on paiement(abonnement_id);
