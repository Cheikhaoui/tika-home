-- Per-email Reply-To: guest emails reply to the owner, the owner's request email replies to the guest.
ALTER TABLE email_outbox ADD COLUMN reply_to VARCHAR(320);
