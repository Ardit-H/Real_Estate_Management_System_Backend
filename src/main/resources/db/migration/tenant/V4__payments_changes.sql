ALTER TABLE payments
    ADD recipient_id BIGINT NULL;

ALTER TABLE payments
    ADD CONSTRAINT FK_PAYMENTS_ON_RECIPIENT FOREIGN KEY (recipient_id) REFERENCES public.users (id);