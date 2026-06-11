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
 * 写入新文件的工具。安全策略：若文件已存在则拒绝覆盖。
 */
public class WriteFileTool extends BaseTool {

    @Override
    public String getName() {
        return "write_file";
    }

    @Override
    public String formatArgsPreview(Map<String, Object> args) {
        if (args == null) return "";
        String filePath = (String) args.get("file_path");
        Object contentObj = args.get("content");
        String content = contentObj != null ? contentObj.toString() : "";

        StringBuilder sb = new StringBuilder();
        if (filePath != null) {
            sb.append(filePath).append(" (new file)\n");
        }

        if (!content.isEmpty()) {
            String[] lines = content.split("\n", -1);
            int maxLines = Math.min(lines.length, 10);
            for (int i = 0; i < maxLines; i++) {
                sb.append(String.format("%6d  + %s\n", i + 1, truncateLine(lines[i])));
            }
            if (lines.length > 10) {
                sb.append(String.format("  ... (%d more lines)\n", lines.length - 10));
            }
        }

        return sb.toString().trim();
    }

    private String truncateLine(String line) {
        if (line.length() > 80) {
            return line.substring(0, 77) + "...";
        }
        return line;
    }

    @Override
    public String getDescription() {
        return "Write content to a new file. Fails if the file already exists to prevent accidental overwrites.";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> props = new HashMap<>();
        props.put("type", "object");

        Map<String, Object> filePath = new HashMap<>();
        filePath.put("type", "string");
        filePath.put("description", "Absolute path to the file to create");

        Map<String, Object> content = new HashMap<>();
        content.put("type", "string");
        content.put("description", "Content to write to the file");

        Map<String, Object> properties = new HashMap<>();
        properties.put("file_path", filePath);
        properties.put("content", content);

        props.put("properties", properties);
        props.put("required", java.util.Arrays.asList("file_path", "content"));
        return props;
    }

    @Override
    public String execute(Map<String, Object> params) {
        String filePath = (String) params.get("file_path");
        if (filePath == null || filePath.isEmpty()) {
            return "[Error: file_path is required]";
        }

        Object contentObj = params.get("content");
        if (contentObj == null) {
            return "[Error: content is required]";
        }
        String content = contentObj.toString();

        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (Files.exists(path)) {
            return "[Error: file already exists: " + path + "]";
        }

        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            return "File created successfully: " + path;
        } catch (Exception e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }
}
