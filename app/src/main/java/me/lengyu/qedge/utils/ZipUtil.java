package me.lengyu.qedge.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip 压缩/解压缩工具类
 * <p>
 * 实现思路参照 QStory 的 {@code lin.xposed.common.utils.ZipUtil}，
 * 保留递归创建目录、流式缓冲写入的简单写法，适配不同系统。
 * 区别在于解压默认使用 UTF-8 编码，避免中文文件名在部分系统上乱码或解包失败。
 * </p>
 */
public final class ZipUtil {
    private static final int BUFFER_SIZE = 2 * 1024;

    private ZipUtil() {
    }

    /**
     * 解压
     *
     * @param zipFilePath  待解压文件完整路径
     * @param desDirectory 解压到的目标目录
     * @throws Exception 解压失败时抛出
     */
    public static void unzip(String zipFilePath, String desDirectory) throws Exception {
        unzip(zipFilePath, desDirectory, StandardCharsets.UTF_8);
    }

    /**
     * 解压，允许指定编码
     *
     * @param zipFilePath  待解压文件完整路径
     * @param desDirectory 解压到的目标目录
     * @param charset      压缩包文件名编码（zip 内文件名按该编码解码）
     * @throws Exception 解压失败时抛出
     */
    public static void unzip(String zipFilePath, String desDirectory, Charset charset) throws Exception {
        File desDir = new File(desDirectory);
        if (!desDir.exists() && !desDir.mkdirs()) {
            throw new IOException("创建解压目标文件夹失败: " + desDirectory);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath), charset)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String unzipFilePath = desDirectory + File.separator + zipEntry.getName();
                File file = new File(unzipFilePath);

                // 防 Zip Slip：确保解出的文件仍在目标目录内
                if (!file.getCanonicalPath().startsWith(desDir.getCanonicalPath())) {
                    throw new IOException("非法压缩路径: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) { // 文件夹
                    mkdir(file);
                } else { // 文件
                    // 创建父目录
                    mkdir(file.getParentFile());
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] bytes = new byte[1024];
                        int readLen;
                        while ((readLen = zis.read(bytes)) != -1) {
                            bos.write(bytes, 0, readLen);
                        }
                    }
                }
            }
        }
    }

    // 若父目录不存在则递归创建
    private static void mkdir(File file) {
        if (null == file || file.exists()) {
            return;
        }
        mkdir(file.getParentFile());
        file.mkdir();
    }

    /**
     * 压缩成 ZIP
     *
     * @param srcDir           压缩源文件夹路径
     * @param out              压缩文件输出流
     * @param keepDirStructure 是否保留原来的目录结构
     */
    public static void toZip(String srcDir, OutputStream out, boolean keepDirStructure) throws RuntimeException {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(out, StandardCharsets.UTF_8);
            File sourceFile = new File(srcDir);
            compress(sourceFile, zos, sourceFile.getName(), keepDirStructure);
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtil", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void compress(File sourceFile, ZipOutputStream zos, String name, boolean keepDirStructure) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        if (sourceFile.isFile()) {
            zos.putNextEntry(new ZipEntry(name));
            try (FileInputStream in = new FileInputStream(sourceFile)) {
                int len;
                while ((len = in.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
            }
            zos.closeEntry();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                if (keepDirStructure) {
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    zos.closeEntry();
                }
            } else {
                for (File file : listFiles) {
                    if (keepDirStructure) {
                        compress(file, zos, name + "/" + file.getName(), true);
                    } else {
                        compress(file, zos, file.getName(), false);
                    }
                }
            }
        }
    }

    /**
     * 将整个文件夹压缩为 zip 文件
     *
     * @param sourceFilePath 源文件夹路径
     * @param toPath         生成的 zip 文件路径（含文件名）
     * @throws Exception 压缩失败时抛出
     */
    public static void fileToZip(String sourceFilePath, String toPath) throws Exception {
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            throw new RuntimeException("源目录不存在: " + sourceFilePath);
        }
        if (!sourceFile.isDirectory()) {
            throw new RuntimeException("源不是文件夹: " + sourceFilePath);
        }
        File zipFile = new File(toPath);
        if (zipFile.getParentFile() != null && !zipFile.getParentFile().exists()) {
            zipFile.getParentFile().mkdirs();
        }
        if (!zipFile.exists() && !zipFile.createNewFile()) {
            throw new IOException("创建 zip 文件失败: " + toPath);
        }
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)), StandardCharsets.UTF_8)) {
            fileToZip(zos, sourceFile, "");
        }
    }

    private static void fileToZip(ZipOutputStream zos, File sourceFile, String path) throws IOException {
        if (sourceFile.isDirectory()) {
            path = path + sourceFile.getName() + "/";
            ZipEntry zipEntry = new ZipEntry(path);
            zos.putNextEntry(zipEntry);
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileToZip(zos, file, path);
                }
            }
        } else {
            zos.putNextEntry(new ZipEntry(path + sourceFile.getName()));
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile), 1024 * 10)) {
                byte[] bufs = new byte[1024 * 10];
                int read;
                while ((read = bis.read(bufs, 0, 1024 * 10)) != -1) {
                    zos.write(bufs, 0, read);
                }
            }
        }
    }
}