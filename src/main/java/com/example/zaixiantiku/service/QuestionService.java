package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.dto.QuestionAuditDTO;
import com.example.zaixiantiku.pojo.dto.QuestionQueryDTO;
import com.example.zaixiantiku.pojo.dto.QuestionSaveDTO;
import com.example.zaixiantiku.pojo.vo.ImportResultVO;
import com.example.zaixiantiku.pojo.vo.PageResult;
import com.example.zaixiantiku.pojo.vo.QuestionDetailVO;
import com.example.zaixiantiku.pojo.vo.QuestionListVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {

    QuestionDetailVO createQuestion(QuestionSaveDTO saveDTO);

    QuestionDetailVO updateQuestion(Long questionId, QuestionSaveDTO saveDTO);

    void deleteQuestion(Long questionId);

    PageResult<QuestionListVO> getQuestionPage(QuestionQueryDTO queryDTO);

    QuestionDetailVO getQuestionDetail(Long questionId);

    ImportResultVO importQuestions(MultipartFile file, Long courseId);

    void exportQuestions(QuestionQueryDTO queryDTO, HttpServletResponse response);

    void auditQuestion(Long questionId, QuestionAuditDTO auditDTO);
}

