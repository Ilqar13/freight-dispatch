-- Maşın pasportu, sürücü qeydiyyatı, müştəri qeydiyyatı.
-- Köhnə yazılar qorunur: yeni sütunlar ya boşdur, ya da defolt dəyəri var.

-- ------------------------------------------------------------------ maşın
ALTER TABLE truck
    ADD COLUMN brand                  VARCHAR(50),
    ADD COLUMN model                  VARCHAR(50),
    ADD COLUMN manufacture_year       INT,
    ADD COLUMN vin                    VARCHAR(17),
    ADD COLUMN body_length_m          DOUBLE PRECISION,
    ADD COLUMN body_width_m           DOUBLE PRECISION,
    ADD COLUMN body_height_m          DOUBLE PRECISION,
    ADD COLUMN volume_m3              DOUBLE PRECISION,
    ADD COLUMN axles                  INT,
    ADD COLUMN fuel_type              VARCHAR(20),
    ADD COLUMN owner_type             VARCHAR(20) NOT NULL DEFAULT 'COMPANY',
    ADD COLUMN has_tail_lift          BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN adr_certified          BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN temp_min               DOUBLE PRECISION,
    ADD COLUMN temp_max               DOUBLE PRECISION,
    ADD COLUMN gps_device_id          VARCHAR(50),
    ADD COLUMN insurance_expiry       DATE,
    ADD COLUMN tech_inspection_expiry DATE,
    ADD COLUMN last_service_date      DATE,
    ADD COLUMN odometer_km            INT,
    ADD COLUMN notes                  VARCHAR(1000);
CREATE UNIQUE INDEX truck_vin_uidx ON truck (vin) WHERE vin IS NOT NULL;

-- ------------------------------------------------------------------ sürücü
ALTER TABLE driver
    ADD COLUMN national_id            VARCHAR(10),
    ADD COLUMN birth_date             DATE,
    ADD COLUMN license_number         VARCHAR(20),
    ADD COLUMN license_categories     VARCHAR(30),
    ADD COLUMN license_expiry         DATE,
    ADD COLUMN experience_years       INT,
    ADD COLUMN address                VARCHAR(300),
    ADD COLUMN email                  VARCHAR(200),
    ADD COLUMN emergency_contact_name  VARCHAR(200),
    ADD COLUMN emergency_contact_phone VARCHAR(30),
    ADD COLUMN hire_date              DATE,
    ADD COLUMN medical_expiry         DATE,
    ADD COLUMN adr_certified          BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN notes                  VARCHAR(1000);
CREATE UNIQUE INDEX driver_national_id_uidx ON driver (national_id) WHERE national_id IS NOT NULL;
CREATE UNIQUE INDEX driver_license_uidx ON driver (license_number) WHERE license_number IS NOT NULL;

-- ------------------------------------------------------------------ müştəri
CREATE TABLE customer (
    id                  BIGSERIAL PRIMARY KEY,
    customer_type       VARCHAR(20)  NOT NULL,
    name                VARCHAR(200) NOT NULL,
    tax_id              VARCHAR(10),
    legal_address       VARCHAR(500),
    contact_name        VARCHAR(200) NOT NULL,
    phone               VARCHAR(30)  NOT NULL UNIQUE,
    email               VARCHAR(200),
    bank_name           VARCHAR(100),
    bank_account        VARCHAR(34),
    contract_no         VARCHAR(50),
    payment_terms_days  INT          NOT NULL DEFAULT 0,
    credit_limit        DOUBLE PRECISION,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    password_hash       VARCHAR(100) NOT NULL,
    failed_attempts     INT          NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    notes               VARCHAR(1000),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX customer_tax_id_uidx ON customer (tax_id) WHERE tax_id IS NOT NULL;

CREATE TABLE customer_session (
    token_hash  VARCHAR(64) PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX customer_session_customer_idx ON customer_session (customer_id);
CREATE INDEX customer_session_expires_idx ON customer_session (expires_at);

ALTER TABLE orders ADD COLUMN customer_id BIGINT REFERENCES customer (id);
CREATE INDEX orders_customer_idx ON orders (customer_id, created_at DESC);
