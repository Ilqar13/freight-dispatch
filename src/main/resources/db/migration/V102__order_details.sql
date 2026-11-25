-- Real logistika sifarişi: yükləmə/çatdırılma tərəfləri, vaxt pəncərələri, yükün xüsusiyyətləri, ödəniş.
-- Köhnə sifarişlər qorunur: bütün yeni sütunlar ya boş ola bilir, ya da defolt dəyəri var.

ALTER TABLE orders
    ADD COLUMN order_no               VARCHAR(30),
    -- Müştəri / sifarişçi
    ADD COLUMN contact_name           VARCHAR(200),
    ADD COLUMN contact_phone          VARCHAR(30),
    ADD COLUMN contact_email          VARCHAR(200),
    ADD COLUMN tax_id                 VARCHAR(20),
    ADD COLUMN reference_no           VARCHAR(100),
    -- Yükləmə nöqtəsi
    ADD COLUMN pickup_address         VARCHAR(500),
    ADD COLUMN pickup_lat             DOUBLE PRECISION,
    ADD COLUMN pickup_lon             DOUBLE PRECISION,
    ADD COLUMN pickup_contact_name    VARCHAR(200),
    ADD COLUMN pickup_contact_phone   VARCHAR(30),
    ADD COLUMN pickup_from            TIMESTAMPTZ,
    ADD COLUMN pickup_to              TIMESTAMPTZ,
    -- Çatdırılma nöqtəsi (ünvan və koordinat dest_* sütunlarındadır)
    ADD COLUMN dest_contact_name      VARCHAR(200),
    ADD COLUMN dest_contact_phone     VARCHAR(30),
    ADD COLUMN delivery_from          TIMESTAMPTZ,
    ADD COLUMN delivery_to            TIMESTAMPTZ,
    -- Yük
    ADD COLUMN cargo_description      VARCHAR(1000),
    ADD COLUMN volume_m3              DOUBLE PRECISION,
    ADD COLUMN packages_count         INT,
    ADD COLUMN package_type           VARCHAR(20),
    ADD COLUMN declared_value         DOUBLE PRECISION,
    ADD COLUMN hazardous              BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN un_number              VARCHAR(20),
    ADD COLUMN temp_min               DOUBLE PRECISION,
    ADD COLUMN temp_max               DOUBLE PRECISION,
    ADD COLUMN fragile                BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN stackable              BOOLEAN NOT NULL DEFAULT true,
    -- Xidmətlər
    ADD COLUMN needs_loading_help     BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN needs_unloading_help   BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN needs_insurance        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN special_instructions   VARCHAR(2000),
    -- Ödəniş
    ADD COLUMN price                  DOUBLE PRECISION,
    ADD COLUMN currency               VARCHAR(3) NOT NULL DEFAULT 'AZN',
    ADD COLUMN payment_method         VARCHAR(20),
    ADD COLUMN payer                  VARCHAR(20),
    ADD COLUMN invoice_required       BOOLEAN NOT NULL DEFAULT false;

-- Köhnə sifarişlərə də nömrə: 166-2026-00005
UPDATE orders
SET order_no = '166-' || extract(year FROM created_at)::int || '-' || lpad(id::text, 5, '0')
WHERE order_no IS NULL;

CREATE UNIQUE INDEX orders_order_no_uidx ON orders (order_no);
CREATE INDEX orders_status_created_idx ON orders (status, created_at DESC);
