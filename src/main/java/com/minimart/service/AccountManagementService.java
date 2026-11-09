package com.minimart.service;

import com.minimart.dto.request.ManagedAccountRequest;
import com.minimart.entity.UserAccount;
import java.util.List;

public interface AccountManagementService {
    List<UserAccount> list();
    UserAccount get(Long id);
    void create(ManagedAccountRequest request);
    void update(Long id, ManagedAccountRequest request);
    void setActive(Long id, boolean active);
    void delete(Long id);
}
