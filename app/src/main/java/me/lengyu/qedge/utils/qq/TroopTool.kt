package me.lengyu.qedge.utils.qq

import me.lengyu.qedge.plugin.bean.ForbidInfo
import me.lengyu.qedge.plugin.bean.GroupInfo
import me.lengyu.qedge.plugin.bean.MemberInfo
import me.lengyu.qedge.utils.reflect.ClassUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.proto.PacketHelper
import me.lengyu.qedge.utils.proto.packetListener
import me.lengyu.qedge.utils.reflect.findMethod
import me.lengyu.qedge.utils.reflect.findMethodOrNull
import org.json.JSONObject
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import com.tencent.mobileqq.data.troop.TroopInfo
import com.tencent.mobileqq.data.troop.TroopMemberInfo
import com.tencent.mobileqq.data.troop.TroopMemberCardInfo
import com.tencent.mobileqq.troop.api.ITroopInfoService

@Suppress("DEPRECATION")
/**
 * @Author 冷雨
 * @Description 群工具类，需要dexkit查找
 */
object TroopTool : DexKitTask {

    private val modifyTroopShutUpTime by lazy {
        Class.forName("com.tencent.qqnt.troop.ITroopOperationRepoApi").findMethod {
            name = "modifyTroopShutUpTime"
        }
    }

    private val fetchTroopMemberList by lazy {
        Class.forName("com.tencent.qqnt.troopmemberlist.ITroopMemberListRepoApi").findMethod {
            name = "fetchTroopMemberList"
            paramCount = 5
        }
    }

    private val fetchTroopMemberInfo by lazy {
        Class.forName("com.tencent.qqnt.troopmemberlist.ITroopMemberListRepoApi").findMethod {
            name = "fetchTroopMemberInfo"
            paramCount = 6
        }
    }

    private val troopMemberInfoClass by lazy {
        Class.forName("com.tencent.mobileqq.data.troop.TroopMemberInfo")
    }

    private val shutUp by lazy {
        val handler = Class.forName("com.tencent.mobileqq.troop.membersetting.handler.MemberSettingHandler")
        handler.findMethodOrNull {
            returnType = boolean
            paramTypes(string, string, long)
        } ?: handler.findMethod {
            returnType = boolean
            paramTypes(long, string, string)
        }
    }

    private val setGroupAdmin by lazy {
        requireClass("setting").findMethod {
            returnType = void
            paramTypes(byte, string, string)
        }
    }

    private val changeMemberName by lazy {
        Class.forName("com.tencent.mobileqq.troop.handler.TroopMemberCardHandler").findMethod {
            returnType = void
            paramTypes(string, arrayList, arrayList)
        }
    }

    private val clockIn by lazy {
        Class.forName("com.tencent.mobileqq.troop.clockin.handler.TroopClockInHandler").findMethod {
            returnType = void
            paramTypes(string, string)
        }
    }

    fun clockIn(troopUin: String) {
        clockIn.invoke(
            QQServiceHelper.getHandler(Class.forName("com.tencent.mobileqq.troop.clockin.handler.TroopClockInHandler")),
            troopUin,
            QQCurrentEnv.getCurrentUin()
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun getGroupList(): List<GroupInfo> {
        val groupInfoList = mutableListOf<GroupInfo>()
        try {
            val service = QQServiceHelper.getApi(Class.forName("com.tencent.qqnt.troop.ITroopListRepoApi"))
                ?: return groupInfoList

            // 直接强转为 List<TroopInfo>，随后全部直接访问字段/方法
            val troopList = me.lengyu.qedge.utils.ReflectUtils.callMethod(service, "getSortedJoinedTroopInfoFromCache")
                as? List<TroopInfo> ?: return groupInfoList

            for (troop in troopList) {
                try {
                    val troopUin = troop.troopuin ?: continue
                    val troopName = troop.troopNameFromNT ?: troopUin
                    val troopOwnerUin = troop.troopowneruin ?: ""
                    groupInfoList.add(GroupInfo(troopUin, troopName, troopOwnerUin, troop))
                } catch (e: Throwable) {
                    LogUtils.e(e)
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(e)
        }
        return groupInfoList
    }

    fun getGroupInfo(troopUin: String): TroopInfo {
        return try {
            val app = QQServiceHelper.getRuntime() ?: return TroopInfo()
            app.getRuntimeService(ITroopInfoService::class.java, "")?.getTroopInfo(troopUin) ?: TroopInfo()
        } catch (e: Throwable) {
            LogUtils.e(e)
            TroopInfo()
        }
    }

    fun shutUpAll(troopUin: String, enable: Boolean) {
        modifyTroopShutUpTime.invoke(
            QQServiceHelper.getApi(Class.forName("com.tencent.qqnt.troop.ITroopOperationRepoApi")),
            troopUin,
            if (enable) 0x0FFFFFFF else 0,
            null,
            null
        )
    }

    fun shutUp(troopUin: String, uin: String, time: Long) {
        val handler = QQServiceHelper.getHandler(Class.forName("com.tencent.mobileqq.troop.membersetting.handler.MemberSettingHandler"))
        runCatching {
            shutUp.invoke(handler, troopUin, uin, time)
        }.onFailure {
            shutUp.invoke(handler, time, troopUin, uin)
        }
    }

    fun setGroupAdmin(troopUin: String, uin: String, enable: Boolean) {
        val byte: Byte = if (enable) 1 else 0
        setGroupAdmin.invoke(
            requireClass("setting").newInstance(),
            byte,
            troopUin,
            uin
        )
    }

    fun kickGroup(troopUin: String, uin: String, block: Boolean) {
        val req = JSONObject().apply {
            put("1", 0x8a0)
            put("2", 0)
            put("3", 0)
            put("4", JSONObject().apply {
                put("1", troopUin.toLong())
                put("2", JSONObject().apply {
                    put("1", 5)
                    put("2", uin.toLong())
                    put("3", if (block) 1 else 0)
                })
            })
        }
        PacketHelper.sendPacket("OidbSvc.0x8a0_0", req, object : packetListener {
            override fun onResult(success: Boolean, json: JSONObject) {}
        })
    }

    fun changeMemberName(troopUin: String, uin: String, name: String) {
        // 直接 new + 直接赋值，不再走反射
        val cardInfo = TroopMemberCardInfo().apply {
            colorNick = ""
            colorNickId = 0
            memberuin = uin
            this.name = name
            this.troopuin = troopUin
        }
        changeMemberName.invoke(
            QQServiceHelper.getHandler(Class.forName("com.tencent.mobileqq.troop.handler.TroopMemberCardHandler")),
            troopUin, arrayListOf(cardInfo), arrayListOf(1)
        )
    }

    fun isShutUp(troopUin: String): Boolean {
        val info = getGroupInfo(troopUin)
        return !(info.dwGagTimeStamp == 0L && info.dwGagTimeStamp_me == 0L)
    }


    private fun processMemberInfo(troopMemberInfo: TroopMemberInfo): MemberInfo {
        val troopNick = troopMemberInfo.troopnick
        val uinName = if (troopNick.isNullOrEmpty()) {
            troopMemberInfo.friendnick
        } else {
            troopNick
        }
        return MemberInfo(
            troopMemberInfo.join_time,
            troopMemberInfo.last_active_time,
            troopMemberInfo.memberuin ?: "",
            troopMemberInfo.realLevel,
            uinName ?: "",
            troopMemberInfo.role.toString(),
            troopMemberInfo
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMemberInfoList(troopUin: String): List<TroopMemberInfo> {
        val completableFuture = CompletableFuture<ArrayList<TroopMemberInfo>>()
        val callback = Proxy.newProxyInstance(
            ClassUtils.hostClassLoader,
            arrayOf(fetchTroopMemberList.parameterTypes[4])
        ) { _, method, args ->
            if (method.returnType == Void.TYPE && method.parameterCount == 2) {
                val list = when {
                    args[0] is ArrayList<*> -> args[0]
                    args[1] is ArrayList<*> -> args[1]
                    else -> emptyList<Any>()
                }
                completableFuture.complete(list as ArrayList<TroopMemberInfo>)
            }
            0
        }
        fetchTroopMemberList.invoke(
            QQServiceHelper.getApi(Class.forName("com.tencent.qqnt.troopmemberlist.ITroopMemberListRepoApi")),
            troopUin, null, true, "", callback
        )
        return completableFuture.get(5, TimeUnit.SECONDS)
    }

    fun getMemberInfo(troopUin: String, uin: String): MemberInfo {
        val completableFuture = CompletableFuture<TroopMemberInfo>()
        val callback = Proxy.newProxyInstance(
            ClassUtils.hostClassLoader,
            arrayOf(fetchTroopMemberInfo.parameterTypes[5])
        ) { _, method, args ->
            if (method.returnType == Void.TYPE && method.parameterTypes[0] == troopMemberInfoClass) {
                completableFuture.complete(args[0] as TroopMemberInfo)
            }
            0
        }
        fetchTroopMemberInfo.invoke(
            QQServiceHelper.getApi(Class.forName("com.tencent.qqnt.troopmemberlist.ITroopMemberListRepoApi")),
            troopUin, uin, true, null, "", callback
        )
        return processMemberInfo(completableFuture.get(5, TimeUnit.SECONDS))
    }

    fun getGroupMemberList(troopUin: String): List<MemberInfo> {
        val memberList = ArrayList<MemberInfo>()
        try {
            getMemberInfoList(troopUin).forEach {
                memberList.add(processMemberInfo(it))
            }
        } catch (e: Throwable) {
            LogUtils.e(e)
        }
        return memberList
    }

    fun getForbidInfo(troopUin: String): List<ForbidInfo> {
        val forbidList = ArrayList<ForbidInfo>()
        try {
            val now = System.currentTimeMillis() / 1000
            getMemberInfoList(troopUin).forEach { member ->
                // gagTimeStamp 是 int，直接访问并转 Long
                val time = member.gagTimeStamp.toLong() - now
                if (time > 0) {
                    val troopNick = member.troopnick
                    val userName = if (troopNick.isNullOrEmpty()) member.friendnick else troopNick
                    forbidList.add(ForbidInfo(member.memberuin ?: "", userName ?: "", time))
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(e)
        }
        return forbidList
    }

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        "setting" to FindClass().apply {
            searchPackages("com.tencent.mobileqq.troop.membersetting.part")
            matcher {
                usingStrings("MemberSettingGroupManagePart")
            }
        }
    )
}
