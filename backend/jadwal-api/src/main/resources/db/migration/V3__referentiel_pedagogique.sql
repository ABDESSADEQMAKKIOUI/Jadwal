-- Référentiel pédagogique : grille horaire, niveaux, groupes, matières, salles, créneaux, barrettes

alter table etablissement
    add column grille_json text,
    add column amplitude_max_unites int not null default 16;

create table niveau (
    id                    bigserial primary key,
    etablissement_id      bigint       not null references etablissement(id),
    libelle               varchar(100) not null,
    cycle                 varchar(50),
    ordre                 int          not null default 0,
    charge_max_unites_jour int         null
);

create index idx_niveau_etablissement on niveau(etablissement_id);

create table groupe (
    id               bigserial primary key,
    etablissement_id bigint       not null references etablissement(id),
    niveau_id        bigint       not null references niveau(id),
    libelle          varchar(100) not null,
    effectif         int          not null default 0,
    parent_id        bigint       null references groupe(id),
    type             varchar(20)  not null default 'CLASSE'
);

create index idx_groupe_etablissement on groupe(etablissement_id);
create index idx_groupe_niveau on groupe(niveau_id);

create table matiere (
    id                    bigserial primary key,
    etablissement_id      bigint       not null references etablissement(id),
    libelle               varchar(100) not null,
    code                  varchar(30)  not null,
    coefficient           int          not null default 1,
    poids_cognitif        int          not null default 3,
    couleur               varchar(7)   not null default '#6366f1',
    type_salle_requis     varchar(50)  null,
    equipements_requis    text         null,
    duree_min_unites      int          not null default 1,
    duree_max_unites      int          not null default 4,
    eviter_avant_dejeuner boolean      not null default false,
    eviter_fin_journee    boolean      not null default false,
    unique(etablissement_id, code)
);

create index idx_matiere_etablissement on matiere(etablissement_id);

create table salle (
    id               bigserial primary key,
    etablissement_id bigint       not null references etablissement(id),
    nom              varchar(100) not null,
    capacite         int          not null default 30,
    type             varchar(50)  not null default 'STANDARD',
    equipements      text         null,
    batiment         varchar(50)  null
);

create index idx_salle_etablissement on salle(etablissement_id);

create table creneau (
    id                  bigserial primary key,
    etablissement_id    bigint      not null references etablissement(id),
    jour                varchar(10) not null,
    index_debut         int         not null,
    type                varchar(15) not null default 'COURS',
    unites_disponibles  int         not null default 1
);

create index idx_creneau_etablissement on creneau(etablissement_id);

create table barrette (
    id               bigserial primary key,
    etablissement_id bigint       not null references etablissement(id),
    libelle          varchar(100) not null,
    matiere_id       bigint       not null references matiere(id)
);

create index idx_barrette_etablissement on barrette(etablissement_id);

create table barrette_groupe (
    barrette_id bigint not null references barrette(id) on delete cascade,
    groupe_id   bigint not null references groupe(id) on delete cascade,
    primary key(barrette_id, groupe_id)
);
