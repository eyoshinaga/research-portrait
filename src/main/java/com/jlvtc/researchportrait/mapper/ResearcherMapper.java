package com.jlvtc.researchportrait.mapper;

import com.jlvtc.researchportrait.entity.Researcher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResearcherMapper {
    
    @Select("SELECT * FROM researcher")
    List<Researcher> findAll();
}
