package cn.jingyier.nail.nailong.repository;

import cn.jingyier.nail.nailong.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM messages WHERE conversation_id = #{conversationId} ORDER BY sequence ASC")
    List<Message> selectByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT COALESCE(MAX(sequence), 0) FROM messages WHERE conversation_id = #{conversationId}")
    int selectMaxSequence(@Param("conversationId") Long conversationId);

    @Select("SELECT COUNT(*) FROM messages WHERE conversation_id = #{conversationId}")
    int countByConversationId(@Param("conversationId") Long conversationId);

    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
