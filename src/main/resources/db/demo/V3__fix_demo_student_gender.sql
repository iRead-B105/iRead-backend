UPDATE students
SET gender = CASE LOWER(gender)
    WHEN 'boy' THEN 'Boy'
    WHEN 'girl' THEN 'Girl'
    ELSE gender
END
WHERE gender IS NOT NULL;
