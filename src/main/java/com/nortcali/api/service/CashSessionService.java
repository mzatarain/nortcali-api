package com.nortcali.api.service;

import com.nortcali.api.dto.request.CloseCashSessionRequest;
import com.nortcali.api.dto.request.OpenCashSessionRequest;
import com.nortcali.api.dto.response.CashSessionResponse;

public interface CashSessionService {

    CashSessionResponse open(Long restaurantId, OpenCashSessionRequest request);

    CashSessionResponse close(Long sessionId, CloseCashSessionRequest request);

    CashSessionResponse getCurrent(Long restaurantId);
}
