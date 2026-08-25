package com.tencent.qqnt.kernel.api;

import com.tencent.qqnt.kernel.api.impl.MsgService;
import com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession;

import mqq.app.api.IRuntimeService;

public interface IKernelService extends IRuntimeService {
    IQQNTWrapperSession getWrapperSession();
    MsgService getMsgService(); // 新增此行
    Object getRichMediaService(); // 新增此行
}
