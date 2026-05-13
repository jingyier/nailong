package cn.jingyier.nail.nailong.controller;

import cn.jingyier.nail.nailong.entity.vo.MessageSendRequest;
import cn.jingyier.nail.nailong.service.agent.SwordsmanAgent;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class AgentStreamController {

    private final SwordsmanAgent swordsmanAgent;

    public AgentStreamController(SwordsmanAgent swordsmanAgent) {
        this.swordsmanAgent = swordsmanAgent;
    }

    @PostMapping(value = "/api/v1/conversations/{sessionKey}/messages",
            produces = "application/x-ndjson")
    public StreamingResponseBody sendMessage(@PathVariable String sessionKey,
                                             @RequestBody @Valid MessageSendRequest request) {
        return outputStream -> swordsmanAgent.streamResponse(sessionKey, request.getContent(), outputStream);
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}