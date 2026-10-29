package com.minimart.service;

import com.minimart.dto.request.ProfileUpdateRequest;
import com.minimart.dto.response.AccountProfileDto;

public interface AccountService {

    AccountProfileDto getProfile(String email);

    AccountProfileDto updateProfile(String email, ProfileUpdateRequest request);

    String recordSuccessfulLoginAndGetDashboardPath(String email);
}
