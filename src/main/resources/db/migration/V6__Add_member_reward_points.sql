ALTER TABLE auth_account
    ADD COLUMN reward_points INT NOT NULL DEFAULT 0 AFTER enabled;
