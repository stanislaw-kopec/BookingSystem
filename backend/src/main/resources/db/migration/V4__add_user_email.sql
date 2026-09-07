ALTER TABLE app_users ADD COLUMN email VARCHAR(254);

UPDATE app_users
SET email = lower(username) || '@local.invalid';

ALTER TABLE app_users ALTER COLUMN email SET NOT NULL;
ALTER TABLE app_users ADD CONSTRAINT chk_app_users_email_not_blank CHECK (btrim(email) <> '');
CREATE UNIQUE INDEX uq_app_users_email ON app_users (lower(email));
