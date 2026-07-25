-- Pédagogie et planning : maquettes, affectations, pondérations, versions de planning, séances, générations

create table maquette (
    id                 bigserial primary key,
    etablissement_id   bigint      not null references etablissement(id),
    niveau_id          bigint      not null references niveau(id),
    matiere_id         bigint      not null references matiere(id),
    volume_unites      int         not null,
    volume_unites_b    int         null,
    max_par_jour_unites int        not null default 4,
    dedoublement       varchar(10) not null default 'AUCUN',
    nb_sous_groupes    int         not null default 2,
    co_enseignants     int         not null default 1,
    patterns_json      text        null,
    unique(niveau_id, matiere_id)
);

create index idx_maquette_etablissement on maquette(etablissement_id);

create table volume_override (
    id            bigserial primary key,
    groupe_id     bigint not null references groupe(id) on delete cascade,
    matiere_id    bigint not null references matiere(id),
    volume_unites int    not null,
    unique(groupe_id, matiere_id)
);

create table affectation (
    id               bigserial primary key,
    etablissement_id bigint not null,
    groupe_id        bigint not null references groupe(id) on delete cascade,
    matiere_id       bigint not null references matiere(id),
    enseignant_id    bigint not null references enseignant(id),
    volume_unites    int    null,
    unique(groupe_id, matiere_id, enseignant_id)
);

create index idx_affectation_etablissement on affectation(etablissement_id);

create table ponderation (
    id               bigserial primary key,
    etablissement_id bigint      not null references etablissement(id),
    regle            varchar(10) not null,
    poids            int         not null default 1,
    unique(etablissement_id, regle)
);

create table planning_version (
    id               bigserial primary key,
    etablissement_id bigint       not null references etablissement(id),
    libelle          varchar(150) not null,
    active           boolean      not null default false,
    creee_le         timestamptz  not null default now()
);

create index idx_planning_version_etablissement on planning_version(etablissement_id);

create table seance (
    id               bigserial primary key,
    etablissement_id bigint      not null,
    version_id       bigint      not null references planning_version(id) on delete cascade,
    groupe_id        bigint      not null references groupe(id),
    matiere_id       bigint      not null references matiere(id),
    enseignant_id    bigint      null references enseignant(id),
    salle_id         bigint      null references salle(id),
    creneau_id       bigint      null references creneau(id),
    duree_unites     int         not null,
    semaine          varchar(6)  not null default 'TOUTES',
    bloc_alignement  varchar(50) null,
    barrette_id      bigint      null,
    verrouillee      boolean     not null default false
);

create index idx_seance_version on seance(version_id);
create index idx_seance_groupe on seance(groupe_id);
create index idx_seance_enseignant on seance(enseignant_id);

create table generation (
    id                   bigserial primary key,
    etablissement_id     bigint       not null,
    version_id           bigint       null references planning_version(id) on delete set null,
    statut               varchar(15)  not null default 'EN_COURS',
    score                varchar(120) null,
    duree_max_secondes   int          not null default 120,
    rapport_faisabilite  text         null,
    analyse_json         text         null,
    lancee_le            timestamptz  not null default now(),
    terminee_le          timestamptz  null
);

create index idx_generation_etablissement on generation(etablissement_id);
