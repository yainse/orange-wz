package orange.wz.gui;

import com.formdev.flatlaf.util.SystemFileChooser;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeFileDialogUtil {
    /**
     * 弹出跨平台系统原生文件选择器
     * 文件为主，如果用户选择文件夹，也返回
     *
     * @param parent     父组件，可 null
     * @param title      对话框标题
     * @param allowMulti 是否允许多选
     * @param filters    扩展名过滤器，例如 {"txt","md"}，null = 不过滤
     * @return 用户选择的文件列表，取消选择返回空列表
     */
    public static List<File> chooseFile(
            Component parent,
            String title,
            boolean allowMulti,
            String[] filters
    ) {
        List<File> result = new ArrayList<>();

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(allowMulti);

        // 添加扩展名过滤
        if (filters != null && filters.length > 0) {
            String desc = String.join(", ", filters) + " files";
            chooser.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter(desc, filters));
        }

        int res = chooser.showOpenDialog(parent);
        if (res == SystemFileChooser.APPROVE_OPTION) {
            File[] selected = chooser.getSelectedFiles();
            if (selected != null) {
                Collections.addAll(result, selected);
            }
        }

        return result;
    }

    /**
     * 弹出跨平台系统原生文件选择器
     * 文件为主，如果用户选择文件夹，也返回
     *
     * @param parent     父组件，可 null
     * @param title      对话框标题
     * @param allowMulti 是否允许多选
     * @return 用户选择的文件夹列表，取消选择返回空列表
     */
    public static List<File> chooseFolder(
            Component parent,
            String title,
            boolean allowMulti
    ) {
        List<File> result = new ArrayList<>();

        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(allowMulti);

        int res = chooser.showOpenDialog(parent);
        if (res == SystemFileChooser.APPROVE_OPTION) {
            File[] selected = chooser.getSelectedFiles();
            if (selected != null) {
                Collections.addAll(result, selected);
            }
        }

        return result;
    }

    public static List<File> chooseFolder() {
        return chooseFolder(null, "请选择文件夹", true);
    }

    public static List<File> chooseFile() {
        return chooseFile(null, "请选择文件", true, null);
    }

    public static List<File> chooseFile(String[] filters) {
        return chooseFile(null, "请选择文件", true, filters);
    }

    public static File chooseSingleFile(String[] filters) {
        List<File> files = chooseFile(null, "请选择文件", false, filters);
        if (files.isEmpty()) {
            return null;
        }
        return files.getFirst();
    }
}
