package cn.jingyier.nail.nailong.controller;

import cn.jingyier.nail.nailong.common.Result;
import cn.jingyier.nail.nailong.entity.vo.ConversationCreateRequest;
import cn.jingyier.nail.nailong.entity.vo.ConversationVO;
import cn.jingyier.nail.nailong.entity.vo.MessageVO;
import cn.jingyier.nail.nailong.service.ConversationService;
import cn.jingyier.nail.nailong.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService,
                                  MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping
    public Result<ConversationVO> create(@RequestBody @Valid ConversationCreateRequest request) {
        ConversationVO vo = conversationService.createConversation(request.getTitle());
        return Result.ok(vo);
    }

    @GetMapping
    public Result<List<ConversationVO>> list() {
        List<ConversationVO> list = conversationService.listConversations();
        return Result.ok(list);
    }

    @GetMapping("/{sessionKey}")
    public Result<ConversationVO> get(@PathVariable String sessionKey) {
        ConversationVO vo = conversationService.getConversation(sessionKey);
        return Result.ok(vo);
    }

    @DeleteMapping("/{sessionKey}")
    public Result<Void> delete(@PathVariable String sessionKey) {
        conversationService.deleteConversation(sessionKey);
        return Result.ok();
    }

    @GetMapping("/{sessionKey}/messages")
    public Result<List<MessageVO>> getHistory(@PathVariable String sessionKey) {
        ConversationVO conv = conversationService.getConversation(sessionKey);
        var entity = conversationService.getOrCreateConversation(sessionKey);
        List<MessageVO> messages = messageService.getHistory(entity.getId());
        return Result.ok(messages);
    }
}
