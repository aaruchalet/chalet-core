ALTER TABLE booking
    ADD CONSTRAINT chk_booking_date_range
        CHECK (check_out_date > check_in_date);

ALTER TABLE room_type
    ADD CONSTRAINT chk_room_type_positive_price
        CHECK (price_per_night > 0);
