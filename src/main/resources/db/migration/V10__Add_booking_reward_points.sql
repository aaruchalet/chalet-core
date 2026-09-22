ALTER TABLE booking
    ADD COLUMN reward_points_redeemed INT NOT NULL DEFAULT 0 AFTER meal_plan,
    ADD CONSTRAINT chk_booking_reward_points_redeemed
        CHECK (reward_points_redeemed >= 0);
