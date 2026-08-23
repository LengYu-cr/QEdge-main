package me.lengyu.qedge.plugin.bean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import me.lengyu.qedge.plugin.PluginCompiler;
import me.lengyu.qedge.utils.LogUtils;

/**
 * @Author 冷雨
 * @Description Java插件信息
 */
public class PluginInfo {
    private final String id;
    private String name;
    private String version;
    private String author;
    private final String dirPath;
    private boolean isRunning = false;
    private String desc = "";
    private PluginCompiler compiler;

    public PluginInfo(String id, String name, String version, String author, String dirPath) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.dirPath = dirPath;
        this.compiler = new PluginCompiler(this);
        updateFromDisk();
    }

    public void updateFromDisk() {
        try {
            File dir = new File(dirPath);
            File propFile = new File(dir, "info.prop");
            if (propFile.exists()) {
                Properties props = new Properties();
                try (InputStreamReader reader = new InputStreamReader(
                        new java.io.FileInputStream(propFile), "UTF-8")) {
                    props.load(reader);
                }
                this.name = props.getProperty("pluginName", name);
                this.version = props.getProperty("versionCode", version);
                this.author = props.getProperty("author", author);
            }

            File descFile = new File(dir, "desc.txt");
            if (descFile.exists()) {
                this.desc = readFileContent(descFile);
            }
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    private String readFileContent(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            LogUtils.e(e);
        }
        return content.toString();
    }

    public static PluginInfo fromDir(File dir) {
        try {
            File propFile = new File(dir, "info.prop");
            if (!propFile.exists()) return null;

            Properties props = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new java.io.FileInputStream(propFile), "UTF-8")) {
                props.load(reader);
            }

            String id = props.getProperty("id");
            if (id == null || id.isEmpty()) return null;

            return new PluginInfo(
                    id,
                    props.getProperty("pluginName", "Unknown"),
                    props.getProperty("versionCode", "1.0"),
                    props.getProperty("author", "Unknown"),
                    dir.getAbsolutePath()
            );
        } catch (Exception e) {
            LogUtils.e(e);
            return null;
        }
    }

    @Override
    public String toString() {
        return "PluginInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", dirPath='" + dirPath + '\'' +
                ", isRunning=" + isRunning +
                ", desc='" + desc + '\'' +
                ", compiler=" + compiler +
                '}';
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDirPath() { return dirPath; }
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { isRunning = running; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public PluginCompiler getCompiler() { return compiler; }
    public void setCompiler(PluginCompiler compiler) { this.compiler = compiler; }
}