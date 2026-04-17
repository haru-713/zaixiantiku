package com.example.zaixiantiku.service;

import com.example.zaixiantiku.entity.MistakeBook;
import com.example.zaixiantiku.entity.PracticeRecord;
import com.example.zaixiantiku.pojo.dto.PracticeStartDTO;
import com.example.zaixiantiku.pojo.dto.PracticeSubmitDTO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;

import java.util.List;
import java.util.Map;

public interface PracticeService {
    // 练习相关
    Map<String, Object> startPractice(PracticeStartDTO startDTO);

    Map<String, Object> submitPractice(Long practiceId, PracticeSubmitDTO submitDTO);

    PageResult<PracticeRecord> getPracticeRecords(Integer page, Integer size);

    // 错题本相关
    List<Map<String, Object>> getMistakeBook(Long courseId, Integer typeId);

    Map<String, Object> redoMistake(Long mistakeId);

    /**
     * 提交重做结果
     * @param mistakeId 错题ID
     * @param answer 用户输入的答案
     * @return 是否正确
     */
    boolean submitRedo(Long mistakeId, String answer);

    void removeMistake(Long mistakeId);

    MistakeBook updateMistakeNote(Long mistakeId, String note);

    // 收藏相关
    void addFavorite(Long questionId);

    void removeFavorite(Long favoriteId);

    PageResult<QuestionDetailVO> getFavorites(Integer page, Integer size);
}
