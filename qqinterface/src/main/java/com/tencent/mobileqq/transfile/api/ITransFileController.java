package com.tencent.mobileqq.transfile.api;

import com.tencent.mobileqq.transfile.TransferRequest;
import mqq.app.api.IRuntimeService;

public interface ITransFileController extends IRuntimeService {
    boolean transferAsync(TransferRequest transferRequest);
}