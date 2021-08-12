CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

create table narmeste_leder
(
    narmeste_leder_id            uuid                   primary key,
    orgnummer                    VARCHAR                    not null,
    bruker_fnr                   VARCHAR                    not null,
    narmeste_leder_fnr           VARCHAR                    not null,
    narmeste_leder_telefonnummer VARCHAR                    not null,
    narmeste_leder_epost         VARCHAR                    not null,
    arbeidsgiver_forskutterer    BOOLEAN,
    aktiv_fom                    DATE                       not null,
    timestamp                    TIMESTAMP with time zone   not null
);

create index narmeste_leder_fnr_idx on narmeste_leder (bruker_fnr);
