

CREATE OR REPLACE FUNCTION update_search_vector()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector(
            'simple',
            COALESCE(NEW.title, '')                      || ' ' ||
            COALESCE(NEW.description, '')                || ' ' ||
            COALESCE(CAST(NEW.type AS text), '')         || ' ' ||
            COALESCE(CAST(NEW.listing_type AS text), '')
                         );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_properties_search ON properties;

CREATE TRIGGER trg_properties_search
    BEFORE INSERT OR UPDATE ON properties
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

UPDATE properties
SET title = title
WHERE deleted_at IS NULL;