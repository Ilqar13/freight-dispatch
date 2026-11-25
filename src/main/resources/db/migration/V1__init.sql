CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE truck (
    id                  BIGSERIAL PRIMARY KEY,
    plate               VARCHAR(20)      NOT NULL UNIQUE,
    type                VARCHAR(20)      NOT NULL,
    capacity_tons       DOUBLE PRECISION NOT NULL,
    cost_per_km         DOUBLE PRECISION NOT NULL,
    status              VARCHAR(20)      NOT NULL DEFAULT 'FREE',
    lat                 DOUBLE PRECISION NOT NULL,
    lon                 DOUBLE PRECISION NOT NULL,
    geom                geometry(Point, 4326) GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lon, lat), 4326)) STORED,
    position_updated_at TIMESTAMPTZ      NOT NULL DEFAULT now(),
    version             BIGINT           NOT NULL DEFAULT 0
);
CREATE INDEX truck_geog_idx ON truck USING GIST ((geom::geography));
CREATE INDEX truck_status_type_idx ON truck (status, type);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(200)     NOT NULL,
    cargo_type    VARCHAR(30)      NOT NULL,
    tons          DOUBLE PRECISION NOT NULL,
    dest_address  VARCHAR(500),
    dest_lat      DOUBLE PRECISION NOT NULL,
    dest_lon      DOUBLE PRECISION NOT NULL,
    urgent        BOOLEAN          NOT NULL DEFAULT false,
    status        VARCHAR(20)      NOT NULL,
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE TABLE assignment (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT           NOT NULL REFERENCES orders (id),
    truck_id    BIGINT           NOT NULL REFERENCES truck (id),
    distance_km DOUBLE PRECISION NOT NULL,
    eta_hours   DOUBLE PRECISION NOT NULL,
    cost        DOUBLE PRECISION NOT NULL,
    score       DOUBLE PRECISION NOT NULL,
    reason      TEXT,
    assigned_by VARCHAR(100),
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX assignment_order_idx ON assignment (order_id);
