package cn.jingyier.nail.nailong.service.agent;

import java.io.OutputStream;

public interface CharacterAgent {

    void streamResponse(String sessionKey, String userContent, OutputStream out);
}
