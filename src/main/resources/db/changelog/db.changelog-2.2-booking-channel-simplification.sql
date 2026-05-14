--liquibase formatted sql

--changeset progko:booking-channel-simplification-1
UPDATE orders
SET booking_channel = 'WEB'
WHERE booking_channel = 'WEBSITE';

UPDATE orders
SET booking_channel = 'WALK_IN'
WHERE booking_channel IN ('PHONE', 'WHATSAPP', 'INTERNAL');
--rollback UPDATE orders SET booking_channel = 'WEBSITE' WHERE booking_channel = 'WEB';
--rollback UPDATE orders SET booking_channel = 'INTERNAL' WHERE booking_channel = 'WALK_IN';
