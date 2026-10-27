-- Razorpay and Stripe were replaced by VNPay (PaymentGateway enum = VNPAY only).
-- Done in 3 steps so it also works if old rows exist:
--   1. widen the enum so 'VNPAY' is allowed next to the old values
--   2. move old rows to 'VNPAY'
--   3. narrow the enum to the values the entity knows (Hibernate validate checks this)

ALTER TABLE `payments` MODIFY `provider` ENUM('RAZORPAY','STRIPE','VNPAY') DEFAULT NULL;

UPDATE `payments` SET `provider` = 'VNPAY' WHERE `provider` IN ('RAZORPAY','STRIPE');

ALTER TABLE `payments` MODIFY `provider` ENUM('VNPAY') DEFAULT NULL;
