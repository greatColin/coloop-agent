package com.coloop.agent.capability.tool.filesystem;

import com.coloop.agent.core.tool.BaseTool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于精确字符串替换的文件编辑工具。
 */
public class EditFileTool extends BaseTool {

    @Override
    public String getName() {
        return "edit_file";
    }

    @Override
    public String formatArgsPreview(Map<String, Object> args) {
        if (args == null) return "";
        String filePath = (String) args.get("file_path");
        Object oldObj = args.get("old_string");
        Object newObj = args.get("new_string");
        String oldStr = oldObj != null ? oldObj.toString() : "";
        String newStr = newObj != null ? newObj.toString() : "";

        StringBuilder sb = new StringBuilder();
        if (filePath != null) {
            sb.append(filePath).append("\n");
        }

        // 尝试读取文件并显示带行号的 diff
        if (filePath != null && !oldStr.isEmpty()) {
            try {
                Path path = Paths.get(filePath).toAbsolutePath().normalize();
                if (Files.exists(path) && Files.isRegularFile(path)) {
                    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    int idx = content.indexOf(oldStr);
                    if (idx >= 0) {
                        String before = content.substring(0, idx);
                        int startLine = countLines(before) + 1;
                        String[] lines = content.split("\n", -1);
                        String[] oldLines = oldStr.split("\n", -1);
                        String[] newLines = newStr.split("\n", -1);

                        // 上下文行（变更前2行）
                        int contextStart = Math.max(1, startLine - 2);
                        for (int i = contextStart; i < startLine; i++) {
                            sb.append(String.format("%6d    %s\n", i, truncateLine(lines[i - 1])));
                        }

                        // 删除的行
                        for (int i = 0; i < oldLines.length; i++) {
                            sb.append(String.format("%6d  - %s\n", startLine + i, truncateLine(oldLines[i])));
                        }

                        // 添加的行
                        for (int i = 0; i < newLines.length; i++) {
                            sb.append(String.format("%6d  + %s\n", startLine + i, truncateLine(newLines[i])));
                        }

                        // 上下文行（变更后2行）
                        int afterStart = startLine + oldLines.length;
                        for (int i = 0; i < 2 && afterStart + i <= lines.length; i++) {
                            sb.append(String.format("%6d    %s\n", afterStart + i, truncateLine(lines[afterStart + i - 1])));
                        }

                        return sb.toString().trim();
                    }
                }
            } catch (Exception ignored) {}
        }

        // 回退：简单格式
        if (!oldStr.isEmpty()) {
            for (String line : oldStr.split("\n", 4)) {
                sb.append("- ").append(truncateLine(line)).append("\n");
            }
        }
        if (!newStr.isEmpty()) {
            for (String line : newStr.split("\n", 4)) {
                sb.append("+ ").append(truncateLine(line)).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private int countLines(String s) {
        if (s == null || s.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') count++;
        }
        return count;
    }

    private String truncateLine(String line) {
        if (line.length() > 80) {
            return line.substring(0, 77) + "...";
        }
        return line;
    }

    @Override
    public String getDescription() {
        return "Edit a file by replacing an exact string with a new string. The old_string must match exactly once.";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> props = new HashMap<>();
        props.put("type", "object");

        Map<String, Object> filePath = new HashMap<>();
        filePath.put("type", "string");
        filePath.put("description", "Absolute path to the file to edit");

        Map<String, Object> oldString = new HashMap<>();
        oldString.put("type", "string");
        oldString.put("description", "Exact existing string to replace");

        Map<String, Object> newString = new HashMap<>();
        newString.put("type", "string");
        newString.put("description", "New string to replace the old string with");

        Map<String, Object> properties = new HashMap<>();
        properties.put("file_path", filePath);
        properties.put("old_string", oldString);
        properties.put("new_string", newString);

        props.put("properties", properties);
        props.put("required", java.util.Arrays.asList("file_path", "old_string", "new_string"));
        return props;
    }

    @Override
    public String execute(Map<String, Object> params) {
        String filePath = (String) params.get("file_path");
        if (filePath == null || filePath.isEmpty()) {
            return "[Error: file_path is required]";
        }

        Object oldObj = params.get("old_string");
        Object newObj = params.get("new_string");
        if (oldObj == null) {
            return "[Error: old_string is required]";
        }
        if (newObj == null) {
            return "[Error: new_string is required]";
        }

        String oldString = oldObj.toString();
        String newString = newObj.toString();

        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return "[Error: file not found: " + path + "]";
        }
        if (!Files.isRegularFile(path)) {
            return "[Error: not a regular file: " + path + "]";
        }

        try {
            String original = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            int index = original.indexOf(oldString);
            if (index == -1) {
                return "[Error: old_string not found in file]";
            }
            int lastIndex = original.lastIndexOf(oldString);
            if (lastIndex != index) {
                return "[Error: old_string matches multiple locations in the file; replacement must be unique]";
            }

            String updated = original.substring(0, index) + newString + original.substring(index + oldString.length());
            Files.write(path, updated.getBytes(StandardCharsets.UTF_8));
            return "File edited successfully: " + path;
        } catch (Exception e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }
}
