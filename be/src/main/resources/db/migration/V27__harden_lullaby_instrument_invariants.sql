CREATE OR REPLACE FUNCTION ensure_lullaby_instrument_content_type() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM 1
      FROM contents
     WHERE id = NEW.content_id
       AND type = 'LULLABY'
     FOR KEY SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Lullaby instrument requires LULLABY content: %', NEW.content_id
            USING errcode = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_lullaby_instrument_order(target_content_id bigint) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    instrument_count bigint;
    minimum_order integer;
    maximum_order integer;
BEGIN
    SELECT count(*), min(display_order), max(display_order)
      INTO instrument_count, minimum_order, maximum_order
      FROM lullaby_instruments
     WHERE content_id = target_content_id;

    IF instrument_count > 0
       AND (minimum_order <> 0 OR maximum_order <> instrument_count - 1) THEN
        RAISE EXCEPTION 'Lullaby instrument order must be zero-based and contiguous for content: %',
            target_content_id
            USING errcode = '23514';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION ensure_lullaby_instrument_order() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM validate_lullaby_instrument_order(NEW.content_id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM validate_lullaby_instrument_order(OLD.content_id);
    ELSE
        PERFORM validate_lullaby_instrument_order(NEW.content_id);
        IF OLD.content_id IS DISTINCT FROM NEW.content_id THEN
            PERFORM validate_lullaby_instrument_order(OLD.content_id);
        END IF;
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_lullaby_instruments_contiguous_order
    AFTER INSERT OR UPDATE OR DELETE ON lullaby_instruments
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION ensure_lullaby_instrument_order();
