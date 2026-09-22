package com.tencent.mobileqq.transfile;

public class TransferRequest {
    public int chatType;
    public int delayShowProgressTimeInMs;
    public boolean isShareImageByServer;
    public int mBusiType;
    public boolean mCanSendMsg;
    public int mCommandId;
    public String mDisplayOutFilePath;
    public int mDownMode;
    public byte[] mExtentionInfo;
    public long mFastForwardFileSize;
    public int mFastForwardHeight;
    public int mFastForwardWidth;
    public int mFileType;
    public long mGroupFileID;
    public String mGroupFileKeyStr;
    public boolean mIsPresend;
    public boolean mIsPttPreSend;
    public boolean mIsSecSnapChatPic;
    public boolean mIsUp;
    public String mLocalPath;
    public String mMd5;
    public long mMsgTime;
    public String mOutFilePath;
    public String mPeerUin;
    public int mPicSendSource;
    public int mPttUploadPanel;
    public boolean mReqVideoSubtitle;
    public int mRequestDisplayLength;
    public int mRequestLength;
    public int mRequestOffset;
    public long mSecMsgId;
    public String mSecondId;
    public String mSelfUin;
    public String mServerPath;
    public int mSourceVideoCodecFormat;
    public long mSubMsgId;
    public int mTargetVideoCodecFormat;
    public String mThumbMd5;
    public String mThumbPath;
    public int mUinType;
    public long mUniseq;
    public int multiMsgType;
    public int pcmForVadNum;
    public String pcmForVadPath;
    public int pcmForVadPos;
    public String resIdStr;
    public byte[] toSendData;
    public int upMsgBusiType;
    public String mRichTag;
    public boolean isJubaoMsgType;
    public boolean mIsSelfSend;
    public boolean useOutputstream;
    public boolean mSupportRangeBreakDown;
    public int mDbRecVersion;
    public boolean needSendMsg;
    public int mPrioty;
    public boolean mNeedReport;
    public boolean mIsOnlyGetUrl;
    public boolean mIsFastForward;
    public boolean myPresendInvalid;
    public boolean mPttCompressFinish;
    public boolean bEnableEnc;
    public boolean isQzonePic;

    public TransferRequest() {
    }

    public String getKey() {
        return this.mPeerUin + "_" + this.mFileType + "_" + this.mUniseq + "_" + this.mSubMsgId;
    }

    public String getKeyForTransfer() {
        return this.mPeerUin + this.mUniseq;
    }

    public String toString() {
        return "TransferRequest{mUniseq=" + this.mUniseq + ", mMd5=" + this.mMd5 + ", mIsUp=" + this.mIsUp + "}";
    }
}