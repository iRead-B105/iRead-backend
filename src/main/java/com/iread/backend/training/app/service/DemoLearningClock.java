package com.iread.backend.training.app.service;

import com.iread.backend.training.repository.TrainingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name = "iread.demo-cheat.enabled", havingValue = "true")
public class DemoLearningClock {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final TrainingRepository trainingRepository;
    private final Clock clock;

    @Autowired
    public DemoLearningClock(TrainingRepository trainingRepository) {
        this(trainingRepository, Clock.system(SEOUL));
    }

    DemoLearningClock(TrainingRepository trainingRepository, Clock clock) {
        this.trainingRepository = trainingRepository;
        this.clock = clock;
    }

    public LocalDate currentDate(Long studentId) {
        LocalDate baseDate = baseDate();
        return trainingRepository.findLatestFinishedAtByStudentId(studentId)
                .map(LocalDateTime::toLocalDate)
                .filter(latestDate -> latestDate.isAfter(baseDate))
                .orElse(baseDate);
    }

    public LocalDate baseDate() {
        return LocalDate.now(clock);
    }

    public LocalDateTime currentDateTime(Long studentId) {
        LocalTime currentTime = LocalTime.now(clock);
        return LocalDateTime.of(currentDate(studentId), currentTime);
    }

    public LocalDateTime nextDateTime(Long studentId) {
        return currentDateTime(studentId).plusDays(1);
    }
}
