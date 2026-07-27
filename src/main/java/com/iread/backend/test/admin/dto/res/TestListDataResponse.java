package com.iread.backend.test.admin.dto.res;

import java.util.List;

public record TestListDataResponse(
        List<TestListResponse> testHistory
) {
}
