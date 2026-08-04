UPDATE students
SET image_url = CASE gender
    WHEN 'Boy' THEN '/images/student-profile-boy.png'
    WHEN 'Girl' THEN '/images/student-profile-girl.png'
    ELSE image_url
END
WHERE gender IN ('Boy', 'Girl')
  AND (
      image_url IS NULL
      OR image_url = ''
      OR image_url = '/images/student-profile.png'
  );
