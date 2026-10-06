USE real_estate_db;

-- Safe to run more than once. The script preserves existing records and checks
-- schema metadata before applying either change. Duplicate agreements are
-- reported instead of being deleted or silently altered.
DROP PROCEDURE IF EXISTS migrate_review1_schema;
DELIMITER $$
CREATE PROCEDURE migrate_review1_schema()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'rental_agreements'
          AND column_name = 'application_id' AND non_unique = 0
    ) THEN
        IF EXISTS (
            SELECT application_id FROM rental_agreements
            GROUP BY application_id HAVING COUNT(*) > 1
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Duplicate rental agreements exist; resolve them before adding the one-agreement-per-application constraint.';
        ELSE
            ALTER TABLE rental_agreements
                ADD CONSTRAINT uq_rental_agreements_application UNIQUE (application_id);
        END IF;
    END IF;
END$$
DELIMITER ;

CALL migrate_review1_schema();
DROP PROCEDURE migrate_review1_schema;
