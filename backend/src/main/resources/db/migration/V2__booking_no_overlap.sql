-- THE double-booking guarantee.
--
-- An exclusion constraint is a generalisation of UNIQUE: instead of "no two rows are equal", it says
-- "no two rows satisfy ALL these operators at once". Here: no two rows may have
--   the same property_id   (=)   AND
--   overlapping stay dates (&&)
-- among bookings that hold dates (REQUESTED or CONFIRMED).
--
-- daterange(check_in, check_out, '[)') is half-open: the check-out day is NOT included, so a guest
-- leaving on the 10th and the next one arriving on the 10th do not conflict.
--
-- Postgres checks this at insert/update time using the GiST index it creates, even when two
-- transactions run concurrently: one commits, the other fails with SQLSTATE 23P01 (exclusion_violation).
-- DECLINED / CANCELLED bookings fall outside the WHERE clause, so their dates are released automatically.
ALTER TABLE booking
    ADD CONSTRAINT booking_no_overlap
        EXCLUDE USING gist (
            property_id WITH =,
            daterange(check_in, check_out, '[)') WITH &&
        )
        WHERE (status IN ('REQUESTED', 'CONFIRMED'));
