-- Sürücü portalı: sürücü girişi, nasazlıq (incident) axını, sifariş tarixçəsi.
--
-- Nömrə niyə 101: dev profilində seed miqrasiyası V100 artıq tətbiq oluna bilər; Flyway-də daha kiçik
-- nömrəli yeni miqrasiya "resolved migration not applied" xətası verərdi.

CREATE TABLE driver (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(30)  NOT NULL UNIQUE,
    pin_hash        VARCHAR(100) NOT NULL,
    truck_id        BIGINT REFERENCES truck (id),
    active          BOOLEAN      NOT NULL DEFAULT true,
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Bir maşına eyni anda yalnız bir aktiv sürücü bağlana bilər
CREATE UNIQUE INDEX driver_active_truck_uidx ON driver (truck_id) WHERE truck_id IS NOT NULL AND active;

CREATE TABLE driver_session (
    token_hash VARCHAR(64) PRIMARY KEY,
    driver_id  BIGINT      NOT NULL REFERENCES driver (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX driver_session_driver_idx ON driver_session (driver_id);
CREATE INDEX driver_session_expires_idx ON driver_session (expires_at);

CREATE TABLE incident (
    id                        BIGSERIAL PRIMARY KEY,
    order_id                  BIGINT           NOT NULL REFERENCES orders (id),
    truck_id                  BIGINT           NOT NULL REFERENCES truck (id),
    driver_id                 BIGINT REFERENCES driver (id),
    type                      VARCHAR(30)      NOT NULL,
    description               VARCHAR(1000),
    lat                       DOUBLE PRECISION NOT NULL,
    lon                       DOUBLE PRECISION NOT NULL,
    status                    VARCHAR(30)      NOT NULL,
    replacement_truck_id      BIGINT REFERENCES truck (id),
    replacement_assignment_id BIGINT REFERENCES assignment (id),
    attempts                  INT              NOT NULL DEFAULT 0,
    last_attempt_at           TIMESTAMPTZ,
    created_at                TIMESTAMPTZ      NOT NULL DEFAULT now(),
    replacement_assigned_at   TIMESTAMPTZ,
    closed_at                 TIMESTAMPTZ
);
CREATE INDEX incident_status_idx ON incident (status);
CREATE INDEX incident_order_idx ON incident (order_id);
CREATE INDEX incident_truck_idx ON incident (truck_id);

ALTER TABLE assignment
    ADD COLUMN kind             VARCHAR(20) NOT NULL DEFAULT 'INITIAL',
    ADD COLUMN incident_id      BIGINT REFERENCES incident (id),
    ADD COLUMN pickup_lat       DOUBLE PRECISION,
    ADD COLUMN pickup_lon       DOUBLE PRECISION,
    ADD COLUMN pickup_km        DOUBLE PRECISION,
    ADD COLUMN pickup_eta_hours DOUBLE PRECISION;
CREATE INDEX assignment_truck_idx ON assignment (truck_id);

CREATE TABLE order_event (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT        NOT NULL REFERENCES orders (id),
    type       VARCHAR(30)   NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    actor      VARCHAR(100),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX order_event_order_idx ON order_event (order_id, id);
