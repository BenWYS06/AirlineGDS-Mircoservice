-- Idempotency at the database level: the DB itself refuses duplicates, whatever the code does.
--   booking_id          : one payment per booking (a repeated initiate returns the existing one)
--   transaction_id      : our vnp_TxnRef, the key the IPN looks the payment up by
--   provider_payment_id : VNPay's vnp_TransactionNo; one VNPay transaction can confirm only one payment
--                         (MySQL UNIQUE allows many NULLs, so unpaid rows are fine)
-- checkout_url: kept so a repeated initiate returns the same VNPay link instead of a new one.

ALTER TABLE `payments`
  ADD COLUMN `checkout_url` varchar(2048) DEFAULT NULL,
  ADD CONSTRAINT `uk_payments_booking_id` UNIQUE (`booking_id`),
  ADD CONSTRAINT `uk_payments_transaction_id` UNIQUE (`transaction_id`),
  ADD CONSTRAINT `uk_payments_provider_payment_id` UNIQUE (`provider_payment_id`);
