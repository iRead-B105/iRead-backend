package com.iread.backend.realtime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RealtimeEventHub {

    private static final long RECONNECT_TIME_MILLIS = 1_000L;

    private final ConcurrentMap<Long, Set<SseEmitter>> teacherEmitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<SseEmitter>> studentEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribeTeacher(Long teacherId) {
        return subscribe(teacherEmitters, teacherId);
    }

    public SseEmitter subscribeStudent(Long studentId) {
        return subscribe(studentEmitters, studentId);
    }

    public void publish(Long teacherId, RealtimeEvent event) {
        send(teacherEmitters, teacherId, event);
        send(studentEmitters, event.studentId(), event);
    }

    @Scheduled(fixedDelay = 15_000L)
    public void heartbeat() {
        heartbeat(teacherEmitters);
        heartbeat(studentEmitters);
    }

    int teacherConnectionCount(Long teacherId) {
        return connectionCount(teacherEmitters, teacherId);
    }

    int studentConnectionCount(Long studentId) {
        return connectionCount(studentEmitters, studentId);
    }

    private SseEmitter subscribe(
            ConcurrentMap<Long, Set<SseEmitter>> emitters,
            Long ownerId
    ) {
        SseEmitter emitter = createEmitter();
        emitters.computeIfAbsent(ownerId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable cleanup = () -> remove(emitters, ownerId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .comment("connected")
                    .reconnectTime(RECONNECT_TIME_MILLIS));
        } catch (IOException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    private void send(
            ConcurrentMap<Long, Set<SseEmitter>> emitters,
            Long ownerId,
            RealtimeEvent event
    ) {
        Set<SseEmitter> connections = emitters.get(ownerId);
        if (connections == null) {
            return;
        }
        connections.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventId().toString())
                        .name("student-data-changed")
                        .data(event));
            } catch (Exception exception) {
                remove(emitters, ownerId, emitter);
            }
        });
    }

    private void heartbeat(ConcurrentMap<Long, Set<SseEmitter>> emitters) {
        emitters.forEach((ownerId, connections) -> connections.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception exception) {
                remove(emitters, ownerId, emitter);
            }
        }));
    }

    SseEmitter createEmitter() {
        return new SseEmitter(0L);
    }

    private void remove(
            ConcurrentMap<Long, Set<SseEmitter>> emitters,
            Long ownerId,
            SseEmitter emitter
    ) {
        Set<SseEmitter> connections = emitters.get(ownerId);
        if (connections == null) {
            return;
        }
        connections.remove(emitter);
        if (connections.isEmpty()) {
            emitters.remove(ownerId, connections);
        }
    }

    private int connectionCount(
            ConcurrentMap<Long, Set<SseEmitter>> emitters,
            Long ownerId
    ) {
        Set<SseEmitter> connections = emitters.get(ownerId);
        return connections == null ? 0 : connections.size();
    }
}
