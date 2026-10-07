CREATE TABLE IF NOT EXISTS nacionalidades (
    id      SMALLINT  GENERATED ALWAYS AS IDENTITY,
    clave   TEXT      NOT NULL,   -- codigo ISO de 3 letras del pais (MEX, COL, ...)
    nombre  TEXT      NOT NULL,   -- gentilicio en espanol (Mexicana, Colombiana, ...)

    CONSTRAINT pk_nacionalidades         PRIMARY KEY (id),
    CONSTRAINT uq_nacionalidades_clave   UNIQUE (clave),
    CONSTRAINT uq_nacionalidades_nombre  UNIQUE (nombre),
    CONSTRAINT ck_nacionalidades_clave   CHECK (clave ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_nacionalidades_nombre  CHECK (char_length(nombre) BETWEEN 2 AND 40)
    );
INSERT INTO nacionalidades (clave, nombre) VALUES
                                                       -- America
    ('CHL','Chilena'), ('ARG','Argentina'), ('URY','Uruguaya'), ('PRY','Paraguaya'), ('BRA','Brasileña'),
    ('MEX','Mexicana'), ('USA','Estadounidense'), ('CAN','Canadiense'), ('GTM','Guatemalteca'),
    ('BLZ','Beliceña'), ('SLV','Salvadoreña'), ('HND','Hondureña'), ('NIC','Nicaragüense'),
    ('CRI','Costarricense'), ('PAN','Panameña'), ('CUB','Cubana'), ('DOM','Dominicana'),
    ('HTI','Haitiana'), ('JAM','Jamaiquina'), ('PRI','Puertorriqueña'), ('COL','Colombiana'),
    ('VEN','Venezolana'), ('ECU','Ecuatoriana'), ('PER','Peruana'), ('BOL','Boliviana'),
                                                       -- Europa
    ('ESP','Española'), ('PRT','Portuguesa'), ('FRA','Francesa'), ('ITA','Italiana'), ('DEU','Alemana'),
    ('GBR','Británica'), ('IRL','Irlandesa'), ('NLD','Neerlandesa'), ('BEL','Belga'), ('CHE','Suiza'),
    ('AUT','Austriaca'), ('SWE','Sueca'), ('NOR','Noruega'), ('DNK','Danesa'), ('FIN','Finlandesa'),
    ('POL','Polaca'), ('GRC','Griega'), ('RUS','Rusa'), ('UKR','Ucraniana'), ('ROU','Rumana'),
                                                       -- Asia y Oceania
    ('CHN','China'), ('JPN','Japonesa'), ('KOR','Surcoreana'), ('IND','India'), ('PHL','Filipina'),
    ('VNM','Vietnamita'), ('THA','Tailandesa'), ('IDN','Indonesia'), ('PAK','Pakistaní'), ('ISR','Israelí'),
    ('TUR','Turca'), ('SAU','Saudí'), ('LBN','Libanesa'), ('AUS','Australiana'), ('NZL','Neozelandesa'),
                                                       -- Africa
    ('MAR','Marroquí'), ('EGY','Egipcia'), ('ZAF','Sudafricana'), ('NGA','Nigeriana'), ('KEN','Keniana')
    ON CONFLICT DO NOTHING;

-- La base tambien rechaza nacionalidades que no esten en el catalogo.
ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_nacionalidad FOREIGN KEY (nacionalidad) REFERENCES nacionalidades (nombre);