package com.wizian.web.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.wizian.web.dto.MemberDTO;

@Repository
@Mapper
public interface MemberDAO {

	MemberDTO getMemberById(String id);

	Map<String, Object> userInfo(String userId);


}