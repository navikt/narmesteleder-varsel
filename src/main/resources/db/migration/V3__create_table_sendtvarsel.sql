create table sendt_varsel
(
    sykmelding_id                VARCHAR                 primary key,
    narmeste_leder_id            uuid                       not null,
    bestilling_id                uuid                       not null,
    varseltype                   VARCHAR                    not null,
    timestamp                    TIMESTAMP with time zone   not null
);
