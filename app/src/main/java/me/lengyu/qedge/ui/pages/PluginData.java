package me.lengyu.qedge.ui.pages;

public class PluginData {
    public final String id;
    public final String name;
    public final String version;
    public final String author;
    public final String description;
    public final boolean isRunning;
    public final boolean isAutoLoad;
    public final String dirPath;

    public PluginData(String id, String name, String version, String author, String description, boolean isRunning, boolean isAutoLoad) {
        this(id, name, version, author, description, isRunning, isAutoLoad, "");
    }

    public PluginData(String id, String name, String version, String author, String description, boolean isRunning, boolean isAutoLoad, String dirPath) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.isRunning = isRunning;
        this.isAutoLoad = isAutoLoad;
        this.dirPath = dirPath;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public boolean isRunning() { return isRunning; }
    public boolean isAutoLoad() { return isAutoLoad; }
    public String getDirPath() { return dirPath; }
}
