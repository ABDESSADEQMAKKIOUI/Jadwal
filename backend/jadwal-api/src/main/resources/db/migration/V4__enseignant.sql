-- Enseignants : profils, habilitations, indisponibilités, préférences horaires

create table enseignant (
    id                    bigserial primary key,
    etablissement_id      bigint       not null references etablissement(id),
    nom_complet           varchar(150) not null,
    email                 varchar(150) null,
    type                  varchar(10)  not null default 'PROPRE',
    quota_hebdo_unites    int          not null default 36,
    max_consecutif_unites int          not null default 10,
    amplitude_max_unites  int          null,
    vacataire             boolean      not null default false,
    buffer_trajet_unites  int          not null default 1
);

create index idx_enseignant_etablissement on enseignant(etablissement_id);

create table habilitation (
    id            bigserial primary key,
    enseignant_id bigint not null references enseignant(id) on delete cascade,
    matiere_id    bigint not null references matiere(id),
    unique(enseignant_id, matiere_id)
);

create index idx_habilitation_enseignant on habilitation(enseignant_id);

create table habilitation_niveau (
    habilitation_id bigint not null references habilitation(id) on delete cascade,
    niveau_id       bigint not null references niveau(id),
    primary key(habilitation_id, niveau_id)
);

create table indisponibilite (
    id            bigserial primary key,
    enseignant_id bigint       not null references enseignant(id) on delete cascade,
    source        varchar(15)  not null default 'PERSONNELLE',
    jour          varchar(10)  not null,
    index_debut   int          not null,
    duree_unites  int          not null default 1,
    semaine       varchar(6)   not null default 'TOUTES',
    statut        varchar(10)  not null default 'BROUILLON',
    motif         varchar(200) null
);

create index idx_indisponibilite_enseignant on indisponibilite(enseignant_id);

create table preference_horaire (
    id            bigserial primary key,
    enseignant_id bigint      not null references enseignant(id) on delete cascade,
    jour          varchar(10) not null,
    index_debut   int         not null,
    duree_unites  int         not null default 1,
    type          varchar(10) not null default 'EVITER'
);

create index idx_preference_horaire_enseignant on preference_horaire(enseignant_id);
