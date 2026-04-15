package com.example.zaixiantiku.service;

import com.example.zaixiantiku.pojo.vo.KnowledgePointSimpleVO;
import com.example.zaixiantiku.pojo.dto.KnowledgePointSaveDTO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointTreeVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointVO;
import com.example.zaixiantiku.pojo.vo.KnowledgePointPageVO;
import com.example.zaixiantiku.pojo.vo.PageResult;

import java.util.List;

public interface KnowledgePointService {

    List<KnowledgePointSimpleVO> listByCourse(Long courseId, String keyword);

    KnowledgePointVO create(Long courseId, KnowledgePointSaveDTO saveDTO);

    KnowledgePointVO update(Long kpId, KnowledgePointSaveDTO saveDTO);

    void delete(Long kpId);

    List<KnowledgePointTreeVO> tree(Long courseId);

    KnowledgePointVO detail(Long kpId);

    PageResult<KnowledgePointPageVO> page(Integer page, Integer size, Long courseId, String keyword);

    boolean move(Long kpId, String direction);
}
