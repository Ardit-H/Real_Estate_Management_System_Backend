ALTER TABLE notifications ADD COLUMN is_read_new BOOLEAN DEFAULT FALSE;
UPDATE notifications SET is_read_new = (is_read = B'1');
ALTER TABLE notifications DROP COLUMN is_read;
ALTER TABLE notifications RENAME COLUMN is_read_new TO is_read;