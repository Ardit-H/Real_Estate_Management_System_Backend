ALTER TABLE sale_payments
    ADD recipient_id BIGINT NULL;

ALTER TABLE sale_payments
    ADD CONSTRAINT FK_SALE_PAYMENTS_ON_RECIPIENT FOREIGN KEY (recipient_id) REFERENCES public.users (id);