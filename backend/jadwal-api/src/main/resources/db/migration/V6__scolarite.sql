-- Vie scolaire : élèves et absences, conditionnés par le module d'abonnement

alter table plan_abonnement add column modules varchar(200) not null default 'PLANNING';
update plan_abonnement set modules = 'PLANNING,VIE_SCOLAIRE' where code = 'PREMIUM';

create table eleve (
    id                bigserial primary key,
    etablissement_id  bigint       not null references etablissement(id),
    groupe_id         bigint       null references groupe(id) on delete set null,
    code_massar       varchar(30)  not null,
    nom               varchar(100) not null,
    prenom            varchar(100) not null,
    nom_ar            varchar(100) null,
    prenom_ar         varchar(100) null,
    date_naissance    date         null,
    lieu_naissance    varchar(120) null,
    sexe              varchar(1)   null,
    statut            varchar(20)  not null default 'INSCRIT',
    tuteur_nom        varchar(150) null,
    tuteur_telephone  varchar(30)  null,
    date_creation     timestamptz  not null default now(),
    unique (etablissement_id, code_massar)
);
create index idx_eleve_etablissement on eleve(etablissement_id);
create index idx_eleve_groupe on eleve(groupe_id);

create table absence (
    id                bigserial   primary key,
    etablissement_id  bigint      not null,
    eleve_id          bigint      not null references eleve(id) on delete cascade,
    date_absence      date        not null,
    demi_journee      varchar(12) not null default 'MATIN',
    seance_id         bigint      null references seance(id) on delete set null,
    type              varchar(15) not null default 'ABSENCE',
    justifiee         boolean     not null default false,
    motif             varchar(200) null,
    saisie_par        bigint      null references utilisateur(id),
    date_saisie       timestamptz not null default now()
);
create index idx_absence_etablissement on absence(etablissement_id);
create index idx_absence_eleve_date on absence(eleve_id, date_absence);
create index idx_absence_date on absence(etablissement_id, date_absence);
create unique index uq_absence_demi_journee
    on absence(eleve_id, date_absence, demi_journee) where seance_id is null;
create unique index uq_absence_seance
    on absence(eleve_id, date_absence, seance_id) where seance_id is not null;
