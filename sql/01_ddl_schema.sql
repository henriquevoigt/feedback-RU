-- limpeza pra permitir recriação limpa do esquema durante testes
DROP TABLE IF EXISTS utiliza CASCADE;
DROP TABLE IF EXISTS contem CASCADE;
DROP TABLE IF EXISTS oferece CASCADE;
DROP TABLE IF EXISTS feedback CASCADE;
DROP TABLE IF EXISTS consumo CASCADE;
DROP TABLE IF EXISTS cardapio CASCADE;
DROP TABLE IF EXISTS prato CASCADE;
DROP TABLE IF EXISTS ingrediente CASCADE;
DROP TABLE IF EXISTS restaurante CASCADE;
DROP TABLE IF EXISTS admin CASCADE;
DROP TABLE IF EXISTS servidor CASCADE;
DROP TABLE IF EXISTS aluno CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;

-- HIERARQUIA DE USUÁRIOS (especialização TC)

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL CHECK (TRIM(nome) <> ''),
    email      VARCHAR(150) NOT NULL UNIQUE CHECK (POSITION('@' IN email) > 1)
);

CREATE TABLE aluno (
    id_usuario INTEGER PRIMARY KEY,
    matricula  VARCHAR(30) NOT NULL UNIQUE CHECK (TRIM(matricula) <> ''),
    CONSTRAINT fk_aluno_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE servidor (
    id_usuario INTEGER PRIMARY KEY,
    siape      VARCHAR(30) NOT NULL UNIQUE CHECK (TRIM(siape) <> ''),
    CONSTRAINT fk_servidor_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE admin (
    id_usuario INTEGER PRIMARY KEY,
    crn        VARCHAR(30) NOT NULL UNIQUE CHECK (TRIM(crn) <> ''),
    CONSTRAINT fk_admin_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- CATÁLOGO ALIMENTAR E UNIDADES DO RU

CREATE TABLE restaurante (
    id_rest SERIAL PRIMARY KEY,
    nome    VARCHAR(100) NOT NULL UNIQUE CHECK (TRIM(nome) <> '')
);

CREATE TABLE ingrediente (
    id_ingrediente SERIAL PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL UNIQUE CHECK (TRIM(nome) <> '')
);

CREATE TABLE prato (
    id_prato SERIAL PRIMARY KEY,
    nome     VARCHAR(120) NOT NULL UNIQUE CHECK (TRIM(nome) <> ''),
    calorias INTEGER NOT NULL CHECK (calorias >= 0)
);

-- PLANEJAMENTO DE CARDÁPIOS, CONSUMO (CATRACA) E AVALIAÇÕES (FEEDBACK)

CREATE TABLE cardapio (
    id_cardapio SERIAL PRIMARY KEY,
    data        DATE NOT NULL,
    turno       VARCHAR(20) NOT NULL CHECK (turno IN ('Almoço', 'Jantar')),
    id_admin    INTEGER NOT NULL,
    CONSTRAINT fk_cardapio_admin
        FOREIGN KEY (id_admin) REFERENCES admin (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE consumo (
    id_consumo        SERIAL PRIMARY KEY,
    data_hora_entrada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario        INTEGER NOT NULL,
    id_cardapio       INTEGER NOT NULL,
    CONSTRAINT fk_consumo_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_consumo_cardapio
        FOREIGN KEY (id_cardapio) REFERENCES cardapio (id_cardapio)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE feedback (
    id_feedback SERIAL PRIMARY KEY,
    nota        INTEGER NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario  TEXT,
    data_envio  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_consumo  INTEGER NOT NULL UNIQUE,
    CONSTRAINT fk_feedback_consumo
        FOREIGN KEY (id_consumo) REFERENCES consumo (id_consumo)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- TABELAS ASSOCIATIVAS (relacionamentos N:M)

CREATE TABLE oferece (
    id_cardapio INTEGER NOT NULL,
    id_rest     INTEGER NOT NULL,
    CONSTRAINT pk_oferece PRIMARY KEY (id_cardapio, id_rest),
    CONSTRAINT fk_oferece_cardapio
        FOREIGN KEY (id_cardapio) REFERENCES cardapio (id_cardapio)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_oferece_restaurante
        FOREIGN KEY (id_rest) REFERENCES restaurante (id_rest)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE contem (
    id_cardapio INTEGER NOT NULL,
    id_prato    INTEGER NOT NULL,
    CONSTRAINT pk_contem PRIMARY KEY (id_cardapio, id_prato),
    CONSTRAINT fk_contem_cardapio
        FOREIGN KEY (id_cardapio) REFERENCES cardapio (id_cardapio)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_contem_prato
        FOREIGN KEY (id_prato) REFERENCES prato (id_prato)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE utiliza (
    id_prato       INTEGER NOT NULL,
    id_ingrediente INTEGER NOT NULL,
    CONSTRAINT pk_utiliza PRIMARY KEY (id_prato, id_ingrediente),
    CONSTRAINT fk_utiliza_prato
        FOREIGN KEY (id_prato) REFERENCES prato (id_prato)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_utiliza_ingrediente
        FOREIGN KEY (id_ingrediente) REFERENCES ingrediente (id_ingrediente)
        ON DELETE CASCADE ON UPDATE CASCADE
);
