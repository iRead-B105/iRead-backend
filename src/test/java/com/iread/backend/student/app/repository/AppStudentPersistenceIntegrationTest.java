package com.iread.backend.student.app.repository;

import com.iread.backend.mypage.repository.CharacterRepository;
import com.iread.backend.training.repository.TrainingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AppStudentPersistenceIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TrainingRepository trainingRepository;
    @Autowired CharacterRepository characterRepository;

    @Test
    void aggregatesCompletedTrainingCountAndLoadsCharacterStory() {
        jdbcTemplate.update("""
                INSERT INTO teachers (id, email, password, name, created_at)
                VALUES (901, 'growth-test@example.com', 'encoded', '교사', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO students (id, teacher_id, name, created_at)
                VALUES (902, 901, '학생', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO curriculum_units (id, unit_name, sequence_no)
                VALUES (903, '낱말', 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO training_templates (id, curriculum_unit_id, name, prompt, sequence_no)
                VALUES (904, 903, '낱말 읽기', '{}', 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO daily_curriculums (id, student_id, status, created_at)
                VALUES (905, 902, 'IN_PROGRESS', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO trainings
                    (id, training_template_id, daily_curriculum_id, sequence_no, created_at, status)
                VALUES
                    (906, 904, 905, 1, CURRENT_TIMESTAMP, 'COMPLETED'),
                    (907, 904, 905, 2, CURRENT_TIMESTAMP, 'COMPLETED'),
                    (908, 904, 905, 3, CURRENT_TIMESTAMP, 'NOT_STARTED')
                """);
        jdbcTemplate.update("""
                INSERT INTO story_templates (id, title, content)
                VALUES (909, '이야기', '테스트 이야기')
                """);
        jdbcTemplate.update("""
                INSERT INTO stories
                    (id, student_id, story_template_id, created_at, status, progress)
                VALUES (910, 902, 909, CURRENT_TIMESTAMP, 'IN_PROGRESS', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO characters
                    (id, student_id, image_url, story_id, name, created_at)
                VALUES (911, 902, '/characters/book-fairy.png', 910, '책 요정', CURRENT_TIMESTAMP)
                """);

        var progress = trainingRepository.findCompletedTrainingProgress(902L);
        var characters = characterRepository.findAllByStudentIdOrderByCreatedAtDesc(902L);

        assertThat(progress).singleElement().satisfies(item -> {
            assertThat(item.getTrainingTemplateId()).isEqualTo(904L);
            assertThat(item.getTrainingTemplateName()).isEqualTo("낱말 읽기");
            assertThat(item.getCompletedCount()).isEqualTo(2L);
        });
        assertThat(characters).singleElement().satisfies(character -> {
            assertThat(character.getId()).isEqualTo(911L);
            assertThat(character.getStory().getId()).isEqualTo(910L);
            assertThat(character.getName()).isEqualTo("책 요정");
        });
    }
}
