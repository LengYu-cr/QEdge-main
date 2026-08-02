package com.tencent.qqnt.kernel.api;

import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession;

import mqq.app.api.IRuntimeService;

public interface IKernelService extends IRuntimeService {
    IQQNTWrapperSession getWrapperSession();
    IKernelMsgService getMsgService(); // 新增此行
}
