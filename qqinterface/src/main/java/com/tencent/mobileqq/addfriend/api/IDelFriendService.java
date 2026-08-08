package com.tencent.mobileqq.addfriend.api;

import mqq.app.api.IRuntimeService;

public interface IDelFriendService extends IRuntimeService {
    void delFriend(String callFrom, String friendUin, byte delType, int notShieldTmpSession);
}
