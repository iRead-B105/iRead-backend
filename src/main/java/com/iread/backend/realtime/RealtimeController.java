package com.iread.backend.realtime;

import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.auth.annotation.CurrentTeacherId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class RealtimeController {

    private final RealtimeEventHub eventHub;

    @GetMapping(
            value = "/api/admin/realtime/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribeAdmin(@CurrentTeacherId Long teacherId) {
        return eventHub.subscribeTeacher(teacherId);
    }

    @GetMapping(
            value = "/api/app/realtime/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribeApp(@CurrentStudentId Long studentId) {
        return eventHub.subscribeStudent(studentId);
    }
}
