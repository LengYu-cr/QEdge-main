package me.lengyu.qedge.coldrain.features;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.MsgTool;

/**
 * @Author 冷雨
 * @Description 问答处理
 */
public class QuestionFeature implements ColdRainFeature {

    // 数据文件名
    private static final String FILE_QA_GROUP = "qa_group_%s.dat";      // 本群精准问答
    private static final String FILE_QA_GLOBAL = "qa_global.dat";       // 全局精准问答
    private static final String FILE_FUZZY_GROUP = "fuzzy_group_%s.dat"; // 本群模糊问答
    private static final String FILE_FUZZY_GLOBAL = "fuzzy_global.dat";  // 全局模糊问答

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        if (text.isEmpty()) return false;
        
        // 指令类（管理员指令）
        if (text.equals("问答功能") ||
            text.equals("开启本群精准问答") ||
            text.equals("关闭本群精准问答") ||
            text.startsWith("添加本群精准问#") ||
            text.startsWith("删除本群精准问#") ||
            text.equals("清空本群精准问答") ||
            text.equals("本群精准问答列表") ||
            text.equals("开启全局精准问答") ||
            text.equals("关闭全局精准问答") ||
            text.startsWith("添加全局精准问#") ||
            text.startsWith("删除全局精准问#") ||
            text.equals("清空全局精准问答") ||
            text.equals("全局精准问答列表") ||
            text.equals("开启本群模糊问答") ||
            text.equals("关闭本群模糊问答") ||
            text.startsWith("添加本群模糊问#") ||
            text.startsWith("删除本群模糊问#") ||
            text.equals("清空本群模糊问答") ||
            text.equals("本群模糊问答列表") ||
            text.equals("开启全局模糊问答") ||
            text.equals("关闭全局模糊问答") ||
            text.startsWith("添加全局模糊问#") ||
            text.startsWith("删除全局模糊问#") ||
            text.equals("清空全局模糊问答") ||
            text.equals("全局模糊问答列表") ||
            text.equals("查看变量")) {
            return true;
        }
        
        // 匹配模式：检查是否有已配置的问答
        String qun = msgData.peerUin;
        
        // 获取core实例
        ColdRainCore core = ColdRainCore.getInstance();
        
        // 检查本群精准问答是否开启并匹配
        if ("1".equals(core.getConfigString(qun + "_问答功能_开关", "0"))) {
            JSONObject qaData = core.getDataFile(String.format(FILE_QA_GROUP, qun));
            if (qaData.has(text)) {
                return true;
            }
        }
        
        // 检查全局精准问答是否开启并匹配
        if ("1".equals(core.getConfigString("global_问答功能_开关", "0"))) {
            JSONObject qaData = core.getDataFile(FILE_QA_GLOBAL);
            if (qaData.has(text)) {
                return true;
            }
        }
        
        // 检查本群模糊问答是否开启并匹配
        if ("1".equals(core.getConfigString(qun + "_模糊问答_开关", "0"))) {
            JSONObject fuzzyData = core.getDataFile(String.format(FILE_FUZZY_GROUP, qun));
            String cleanText = cleanText(text);
            for (String key : getKeys(fuzzyData)) {
                if (!key.isEmpty() && cleanText.contains(key)) {
                    return true;
                }
            }
        }
        
        // 检查全局模糊问答是否开启并匹配
        if ("1".equals(core.getConfigString("global_模糊问答_开关", "0"))) {
            JSONObject fuzzyData = core.getDataFile(FILE_FUZZY_GLOBAL);
            String cleanText = cleanText(text);
            for (String key : getKeys(fuzzyData)) {
                if (!key.isEmpty() && cleanText.contains(key)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private ColdRainCore core;
    
    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        this.core = core;
        ModuleScope.launchIOJava("QuestionFeature", () -> {
                try {
                    String text = msgData.msg.trim();
                    String qun = msgData.peerUin;
                    String uin = msgData.userUin;
                    if (uin == null || uin.isEmpty()) uin = msgData.userUid;
                    String qq = core.getMyUin();

                    // 显示菜单
                    if (text.equals("问答功能")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String menu = "问答功能:\n" +
                                    "精准类:\n" +
                                    "开启/关闭本群精准问答\n" +
                                    "添加本群精准问#问#答\n" +
                                    "删除本群精准问#问\n" +
                                    "清空本群精准问答\n" +
                                    "本群精准问答列表\n" +
                                    "开启/关闭全局精准问答\n" +
                                    "添加全局精准问#问#答\n" +
                                    "删除全局精准问#问\n" +
                                    "清空全局精准问答\n" +
                                    "全局精准问答列表\n" +
                                    "模糊类:\n" +
                                    "开启/关闭本群模糊问答\n" +
                                    "添加本群模糊问#问#答\n" +
                                    "删除本群模糊问#问\n" +
                                    "清空本群模糊问答\n" +
                                    "本群模糊问答列表\n" +
                                    "开启/关闭全局模糊问答\n" +
                                    "添加全局模糊问#问#答\n" +
                                    "删除全局模糊问#问\n" +
                                    "清空全局模糊问答\n" +
                                    "全局模糊问答列表\n" +
                                    "变量类:\n" +
                                    "查看变量";
                            core.reply(msgData, menu);
                        }
                        return;
                    }

                    // ========== 本群精准问答 ==========
                    if (text.equals("开启本群精准问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString(qun + "_问答功能_开关", "1");
                            core.reply(msgData, "开启本群精准问答功能成功");
                        }
                        return;
                    }

                    if (text.equals("关闭本群精准问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString(qun + "_问答功能_开关", "0");
                            core.reply(msgData, "关闭本群精准问答功能成功");
                        }
                        return;
                    }

                    if (text.startsWith("添加本群精准问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 3) {
                                String question = parts[1];
                                String answer = parts[2];
                                String fileName = String.format(FILE_QA_GROUP, qun);
                                JSONObject data = core.getDataFile(fileName);
                                data.put(question, answer);
                                core.saveDataFile(fileName, data);
                                core.reply(msgData, "添加成功～");
                            } else {
                                core.reply(msgData, "格式错误！正确格式：添加本群精准问#问#答");
                            }
                        }
                        return;
                    }

                    if (text.startsWith("删除本群精准问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 2) {
                                String question = parts[1];
                                String fileName = String.format(FILE_QA_GROUP, qun);
                                JSONObject data = core.getDataFile(fileName);
                                if (data.has(question)) {
                                    data.remove(question);
                                    core.saveDataFile(fileName, data);
                                    core.reply(msgData, "删除成功～");
                                } else {
                                    core.reply(msgData, "未找到该问答");
                                }
                            } else {
                                core.reply(msgData, "格式错误！正确格式：删除本群精准问#问");
                            }
                        }
                        return;
                    }

                    if (text.equals("清空本群精准问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String fileName = String.format(FILE_QA_GROUP, qun);
                            core.saveDataFile(fileName, new JSONObject());
                            core.reply(msgData, "已清空本群精准问答列表～");
                        }
                        return;
                    }

                    if (text.equals("本群精准问答列表")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String fileName = String.format(FILE_QA_GROUP, qun);
                            JSONObject data = core.getDataFile(fileName);
                            List<String> list = new ArrayList<>(getKeys(data));
                            StringBuilder sb = new StringBuilder();
                            sb.append("本群精准问答列表有:");
                            if (list.isEmpty()) {
                                sb.append("\n目前还没有精准问答哦～");
                            } else {
                                for (int i = 0; i < list.size(); i++) {
                                    sb.append("\n").append(i + 1).append(".").append(list.get(i));
                                }
                            }
                            core.reply(msgData, sb.toString());
                        }
                        return;
                    }

                    // ========== 全局精准问答 ==========
                    if (text.equals("开启全局精准问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_问答功能_开关", "1");
                            core.reply(msgData, "开启全局精准问答功能成功");
                        }
                        return;
                    }

                    if (text.equals("关闭全局精准问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_问答功能_开关", "0");
                            core.reply(msgData, "关闭全局精准问答功能成功");
                        }
                        return;
                    }

                    if (text.startsWith("添加全局精准问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 3) {
                                String question = parts[1];
                                String answer = parts[2];
                                JSONObject data = core.getDataFile(FILE_QA_GLOBAL);
                                data.put(question, answer);
                                core.saveDataFile(FILE_QA_GLOBAL, data);
                                core.reply(msgData, "添加成功～");
                            } else {
                                core.reply(msgData, "格式错误！正确格式：添加全局精准问#问#答");
                            }
                        }
                        return;
                    }

                    if (text.startsWith("删除全局精准问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 2) {
                                String question = parts[1];
                                JSONObject data = core.getDataFile(FILE_QA_GLOBAL);
                                if (data.has(question)) {
                                    data.remove(question);
                                    core.saveDataFile(FILE_QA_GLOBAL, data);
                                    core.reply(msgData, "删除成功～");
                                } else {
                                    core.reply(msgData, "未找到该问答");
                                }
                            } else {
                                core.reply(msgData, "格式错误！正确格式：删除全局精准问#问");
                            }
                        }
                        return;
                    }

                    if (text.equals("清空全局精准问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.saveDataFile(FILE_QA_GLOBAL, new JSONObject());
                            core.reply(msgData, "已清空全局精准问答列表～");
                        }
                        return;
                    }

                    if (text.equals("全局精准问答列表")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            JSONObject data = core.getDataFile(FILE_QA_GLOBAL);
                            List<String> list = new ArrayList<>(getKeys(data));
                            StringBuilder sb = new StringBuilder();
                            sb.append("全局精准问答列表有:");
                            if (list.isEmpty()) {
                                sb.append("\n目前还没有精准问答哦～");
                            } else {
                                for (int i = 0; i < list.size(); i++) {
                                    sb.append("\n").append(i + 1).append(".").append(list.get(i));
                                }
                            }
                            core.reply(msgData, sb.toString());
                        }
                        return;
                    }

                    // ========== 本群模糊问答 ==========
                    if (text.equals("开启本群模糊问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString(qun + "_模糊问答_开关", "1");
                            core.reply(msgData, "开启本群模糊问答功能成功");
                        }
                        return;
                    }

                    if (text.equals("关闭本群模糊问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString(qun + "_模糊问答_开关", "0");
                            core.reply(msgData, "关闭本群模糊问答功能成功");
                        }
                        return;
                    }

                    if (text.startsWith("添加本群模糊问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 3) {
                                String question = parts[1];
                                String answer = parts[2];
                                String fileName = String.format(FILE_FUZZY_GROUP, qun);
                                JSONObject data = core.getDataFile(fileName);
                                data.put(question, answer);
                                core.saveDataFile(fileName, data);
                                core.reply(msgData, "添加成功～");
                            } else {
                                core.reply(msgData, "格式错误！正确格式：添加本群模糊问#问#答");
                            }
                        }
                        return;
                    }

                    if (text.startsWith("删除本群模糊问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 2) {
                                String question = parts[1];
                                String fileName = String.format(FILE_FUZZY_GROUP, qun);
                                JSONObject data = core.getDataFile(fileName);
                                if (data.has(question)) {
                                    data.remove(question);
                                    core.saveDataFile(fileName, data);
                                    core.reply(msgData, "删除成功～");
                                } else {
                                    core.reply(msgData, "未找到该问答");
                                }
                            } else {
                                core.reply(msgData, "格式错误！正确格式：删除本群模糊问#问");
                            }
                        }
                        return;
                    }

                    if (text.equals("清空本群模糊问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String fileName = String.format(FILE_FUZZY_GROUP, qun);
                            core.saveDataFile(fileName, new JSONObject());
                            core.reply(msgData, "已清空本群模糊问答列表～");
                        }
                        return;
                    }

                    if (text.equals("本群模糊问答列表")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String fileName = String.format(FILE_FUZZY_GROUP, qun);
                            JSONObject data = core.getDataFile(fileName);
                            List<String> list = new ArrayList<>(getKeys(data));
                            StringBuilder sb = new StringBuilder();
                            sb.append("本群模糊问答列表有:");
                            if (list.isEmpty()) {
                                sb.append("\n目前还没有模糊问答哦～");
                            } else {
                                for (int i = 0; i < list.size(); i++) {
                                    sb.append("\n").append(i + 1).append(".").append(list.get(i));
                                }
                            }
                            core.reply(msgData, sb.toString());
                        }
                        return;
                    }

                    // ========== 全局模糊问答 ==========
                    if (text.equals("开启全局模糊问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_模糊问答_开关", "1");
                            core.reply(msgData, "开启全局模糊问答功能成功");
                        }
                        return;
                    }

                    if (text.equals("关闭全局模糊问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_模糊问答_开关", "0");
                            core.reply(msgData, "关闭全局模糊问答功能成功");
                        }
                        return;
                    }

                    if (text.startsWith("添加全局模糊问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 3) {
                                String question = parts[1];
                                String answer = parts[2];
                                JSONObject data = core.getDataFile(FILE_FUZZY_GLOBAL);
                                data.put(question, answer);
                                core.saveDataFile(FILE_FUZZY_GLOBAL, data);
                                core.reply(msgData, "添加成功～");
                            } else {
                                core.reply(msgData, "格式错误！正确格式：添加全局模糊问#问#答");
                            }
                        }
                        return;
                    }

                    if (text.startsWith("删除全局模糊问#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String[] parts = text.split("#");
                            if (parts.length >= 2) {
                                String question = parts[1];
                                JSONObject data = core.getDataFile(FILE_FUZZY_GLOBAL);
                                if (data.has(question)) {
                                    data.remove(question);
                                    core.saveDataFile(FILE_FUZZY_GLOBAL, data);
                                    core.reply(msgData, "删除成功～");
                                } else {
                                    core.reply(msgData, "未找到该问答");
                                }
                            } else {
                                core.reply(msgData, "格式错误！正确格式：删除全局模糊问#问");
                            }
                        }
                        return;
                    }

                    if (text.equals("清空全局模糊问答")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.saveDataFile(FILE_FUZZY_GLOBAL, new JSONObject());
                            core.reply(msgData, "已清空全局模糊问答列表～");
                        }
                        return;
                    }

                    if (text.equals("全局模糊问答列表")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            JSONObject data = core.getDataFile(FILE_FUZZY_GLOBAL);
                            List<String> list = new ArrayList<>(getKeys(data));
                            StringBuilder sb = new StringBuilder();
                            sb.append("全局模糊问答列表有:");
                            if (list.isEmpty()) {
                                sb.append("\n目前还没有模糊问答哦～");
                            } else {
                                for (int i = 0; i < list.size(); i++) {
                                    sb.append("\n").append(i + 1).append(".").append(list.get(i));
                                }
                            }
                            core.reply(msgData, sb.toString());
                        }
                        return;
                    }

                    // ========== 查看变量 ==========
                    if (text.equals("查看变量")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String vars = "可用变量:\n" +
                                    "[at] → @发送者\n" +
                                    "[qq] → 机器人QQ\n" +
                                    "[uin] → 发送者QQ\n" +
                                    "[qun] → 群号\n" +
                                    "[Name] → 发送者昵称\n" +
                                    "[GroupName] → 群名称\n" +
                                    "[time] → 发送时间\n" +
                                    "[图片URL] → 图片\n" +
                                    "[GroupMemberCount] → 群成员数\n" +
                                    "[一言] → 随机一言";
                            core.reply(msgData, vars);
                        }
                        return;
                    }

                    // ========== 精准问答匹配 ==========
                    // 先匹配本群精准问答
                    if ("1".equals(core.getConfigString(qun + "_问答功能_开关", "0"))) {
                        JSONObject qaData = core.getDataFile(String.format(FILE_QA_GROUP, qun));
                        if (qaData.has(text)) {
                            String answer = qaData.optString(text, "");
                            if (!answer.isEmpty()) {
                                core.reply(msgData, formatAnswer(answer, uin, qq, qun, msgData.userName));
                                return;
                            }
                        }
                    }
                    // 再匹配全局精准问答
                    if ("1".equals(core.getConfigString("global_问答功能_开关", "0"))) {
                        JSONObject qaData = core.getDataFile(FILE_QA_GLOBAL);
                        if (qaData.has(text)) {
                            String answer = qaData.optString(text, "");
                            if (!answer.isEmpty()) {
                                core.reply(msgData, formatAnswer(answer, uin, qq, qun, msgData.userName));
                                return;
                            }
                        }
                    }

                    // ========== 模糊问答匹配 ==========
                    String cleanText = cleanText(text);
                    
                    // 先匹配本群模糊问答
                    if ("1".equals(core.getConfigString(qun + "_模糊问答_开关", "0"))) {
                        JSONObject fuzzyData = core.getDataFile(String.format(FILE_FUZZY_GROUP, qun));
                        for (String key : getKeys(fuzzyData)) {
                            if (!key.isEmpty() && cleanText.contains(key)) {
                                String answer = fuzzyData.optString(key, "");
                                if (!answer.isEmpty()) {
                                    core.reply(msgData, formatAnswer(answer, uin, qq, qun, msgData.userName));
                                    return;
                                }
                            }
                        }
                    }
                    
                    // 再匹配全局模糊问答
                    if ("1".equals(core.getConfigString("global_模糊问答_开关", "0"))) {
                        JSONObject fuzzyData = core.getDataFile(FILE_FUZZY_GLOBAL);
                        for (String key : getKeys(fuzzyData)) {
                            if (!key.isEmpty() && cleanText.contains(key)) {
                                String answer = fuzzyData.optString(key, "");
                                if (!answer.isEmpty()) {
                                    core.reply(msgData, formatAnswer(answer, uin, qq, qun, msgData.userName));
                                    return;
                                }
                            }
                        }
                    }

                } catch (Throwable e) {
                    LogUtils.e(e);
                }
        });
    }

    private String formatAnswer(String answer, String uin, String qq, String qun, String userName) {
        return answer.replace("[at]", "[atUin=" + uin + "]")
                .replace("[qq]", qq)
                .replace("[uin]", uin)
                .replace("[qun]", qun)
                .replace("[Name]", userName != null ? userName : uin)
                .replace("[图片", "\n[pic=");
    }

    private String cleanText(String text) {
        return text.replace("⁡", "")
                .replace("'", "")
                .replace(" ", "")
                .replace(".", "")
                .replace("•", "")
                .replace(",", "");
    }

    private List<String> getKeys(JSONObject data) {
        List<String> keys = new ArrayList<>();
        java.util.Iterator<String> it = data.keys();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        return keys;
    }
}
