package cn.com.cyber;

import cn.com.cyber.model.CompanyUser;

import java.util.List;

public interface CompanyUserService {

    int deleteByCompanyId(long companyId);

    List<CompanyUser> getUserCompanyInfo(String userId);

}
