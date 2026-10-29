package com.minimart.ui;

import com.minimart.dto.response.AccountProfileDto;
import com.minimart.dto.response.DashboardMetricDto;
import com.minimart.ui.model.DashboardPageModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardPageAssembler {

    public DashboardPageModel assemble(AccountProfileDto account, List<DashboardMetricDto> metrics) {
        return new DashboardPageModel(
                account,
                account.role().getDashboardTitle(),
                account.role().getVietnameseName(),
                metrics
        );
    }
}
