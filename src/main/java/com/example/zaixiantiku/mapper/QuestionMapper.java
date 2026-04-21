package com.example.zaixiantiku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.zaixiantiku.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("<script>" +
            "SELECT q.* FROM question q " +
            "WHERE q.course_id = #{courseId} AND q.status = 2 " +
            "<if test='typeIds != null and typeIds.size() > 0'> " +
            "AND q.type_id IN <foreach collection='typeIds' item='t' open='(' separator=',' close=')'>#{t}</foreach> " +
            "</if>" +
            "<if test='difficulties != null and difficulties.size() > 0'> " +
            "AND q.difficulty IN <foreach collection='difficulties' item='d' open='(' separator=',' close=')'>#{d}</foreach> " +
            "</if>" +
            "<if test='knowledgeIds != null and knowledgeIds.size() > 0'> " +
            "AND q.id IN (SELECT qk.question_id FROM question_knowledge qk WHERE qk.knowledge_point_id IN " +
            "<foreach collection='knowledgeIds' item='k' open='(' separator=',' close=')'>#{k}</foreach>) " +
            "</if>" +
            "ORDER BY RAND() LIMIT #{count}" +
            "</script>")
    List<Question> selectRandomQuestions(
            @Param("courseId") Long courseId,
            @Param("typeIds") List<Integer> typeIds,
            @Param("difficulties") List<Integer> difficulties,
            @Param("knowledgeIds") List<Long> knowledgeIds,
            @Param("count") int count
    );

    @Select("<script>" +
            "SELECT COUNT(*) FROM question q " +
            "WHERE q.course_id = #{courseId} AND q.status = 2 " +
            "<if test='typeIds != null and typeIds.size() > 0'> " +
            "AND q.type_id IN <foreach collection='typeIds' item='t' open='(' separator=',' close=')'>#{t}</foreach> " +
            "</if>" +
            "<if test='difficulties != null and difficulties.size() > 0'> " +
            "AND q.difficulty IN <foreach collection='difficulties' item='d' open='(' separator=',' close=')'>#{d}</foreach> " +
            "</if>" +
            "<if test='knowledgeIds != null and knowledgeIds.size() > 0'> " +
            "AND q.id IN (SELECT qk.question_id FROM question_knowledge qk WHERE qk.knowledge_point_id IN " +
            "<foreach collection='knowledgeIds' item='k' open='(' separator=',' close=')'>#{k}</foreach>) " +
            "</if>" +
            "</script>")
    int countRandomQuestions(
            @Param("courseId") Long courseId,
            @Param("typeIds") List<Integer> typeIds,
            @Param("difficulties") List<Integer> difficulties,
            @Param("knowledgeIds") List<Long> knowledgeIds
    );
}

