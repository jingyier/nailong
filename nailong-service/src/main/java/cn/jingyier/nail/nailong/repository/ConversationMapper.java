package cn.jingyier.nail.nailong.repository;

import cn.jingyier.nail.nailong.entity.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversations WHERE session_key = #{sessionKey}")
    Conversation selectBySessionKey(@Param("sessionKey") String sessionKey);

    @Select("SELECT * FROM conversations WHERE status = 'active' ORDER BY updated_at DESC")
    List<Conversation> selectActiveConversations();

    int updateTitle(@Param("sessionKey") String sessionKey, @Param("title") String title);
}
