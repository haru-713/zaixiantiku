package com.example.zaixiantiku.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.zaixiantiku.entity.QuestionType;
import com.example.zaixiantiku.mapper.QuestionTypeMapper;
import com.example.zaixiantiku.pojo.vo.QuestionTypeVO;
import com.example.zaixiantiku.service.QuestionTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionTypeServiceImpl implements QuestionTypeService {

    private final QuestionTypeMapper questionTypeMapper;

    @Override
    public List<QuestionTypeVO> listEnabledTypes() {
        LambdaQueryWrapper<QuestionType> qw = new LambdaQueryWrapper<>();
        qw.eq(QuestionType::getStatus, 1);
        qw.orderByAsc(QuestionType::getSortOrder).orderByAsc(QuestionType::getId);
        List<QuestionType> list = questionTypeMapper.selectList(qw);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(t -> QuestionTypeVO.builder()
                        .id(t.getId())
                        .typeName(t.getTypeName())
                        .typeCode(t.getTypeCode())
                        .build())
                .collect(Collectors.toList());
    }
}

