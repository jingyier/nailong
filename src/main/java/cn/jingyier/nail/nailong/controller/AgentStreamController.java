package cn.jingyier.nail.nailong.controller;

import cn.jingyier.nail.nailong.entity.vo.MessageSendRequest;
import cn.jingyier.nail.nailong.service.agent.NaiLongAgent;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class AgentStreamController {

    private final NaiLongAgent naiLongAgent;

    public AgentStreamController(NaiLongAgent naiLongAgent) {
        this.naiLongAgent = naiLongAgent;
    }

    @PostMapping(value = "/api/v1/conversations/{sessionKey}/messages",
            produces = "application/x-ndjson")
    public StreamingResponseBody sendMessage(@PathVariable String sessionKey,
                                             @RequestBody @Valid MessageSendRequest request) {
        return outputStream -> naiLongAgent.streamResponse(sessionKey, request.getContent(), outputStream);
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}