ALTER TABLE booking
    ADD COLUMN adults INT NOT NULL DEFAULT 2 AFTER check_out_date,
    ADD COLUMN child_age INT NULL AFTER adults;

ALTER TABLE booking
    ADD CONSTRAINT chk_booking_adults CHECK (adults BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_booking_child_age CHECK (child_age IS NULL OR child_age BETWEEN 0 AND 17);
