package com.terraforged.mod.worldgen.cave;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class CaveDebugReport {
    private final List<String> lines = new ArrayList<>();
    private final List<FeatureRow> features = new ArrayList<>();

    public List<String> lines() {
        return this.lines;
    }

    public List<FeatureRow> features() {
        return this.features;
    }

    public void addLine(String line) {
        this.lines.add(line);
    }

    public void add(String line) {
        this.addLine(line);
    }

    public void addFeature(FeatureRow row) {
        this.features.add(row);
    }

    public void appendFeatureTable() {
        this.lines.add("");
        this.lines.add("[Feature table]");
        if (this.features.isEmpty()) {
            this.lines.add("(no features)");
            return;
        }
        String header = CaveDebugReport.pad("Feature", 48) + " | " + CaveDebugReport.pad("Filter", 8) + " | " + CaveDebugReport.pad("Chamber", 14) + " | " + CaveDebugReport.pad("At feet", 8) + " | " + CaveDebugReport.pad("At anchor", 10) + " | " + CaveDebugReport.pad("Place?", 8) + " | Why";
        this.lines.add(header);
        this.lines.add("-".repeat(Math.min(200, header.length())));
        for (FeatureRow row : this.features) {
            this.lines.add(CaveDebugReport.pad(row.featureId(), 48) + " | " + CaveDebugReport.pad(row.filterStatus(), 8) + " | " + CaveDebugReport.pad(row.chamberStatus(), 14) + " | " + CaveDebugReport.pad(row.feetStatus(), 8) + " | " + CaveDebugReport.pad(row.anchorStatus(), 10) + " | " + CaveDebugReport.pad(row.placeAtAnchor(), 8) + " | " + row.reason());
        }
    }

    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        for (String line : this.lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>NewTerraForged Cave Debug</title>");
        sb.append("<style>body{font-family:Consolas,monospace;background:#111;color:#ddd;padding:16px}");
        sb.append("h2{color:#8cf}pre{white-space:pre-wrap}table{border-collapse:collapse;width:100%;margin-top:12px}");
        sb.append("th,td{border:1px solid #444;padding:6px 8px;text-align:left;vertical-align:top}");
        sb.append("th{background:#222}.ok{color:#8f8}.bad{color:#f88}.warn{color:#fc8}</style></head><body>");
        sb.append("<h2>NewTerraForged Cave Debug</h2><pre>");
        for (String line : this.lines) {
            if (line.startsWith("[Feature table]")) {
                break;
            }
            sb.append(CaveDebugReport.escape(line)).append('\n');
        }
        sb.append("</pre>");
        if (!this.features.isEmpty()) {
            sb.append("<h2>Features</h2><table><tr><th>Feature</th><th>Filter</th><th>Chamber</th><th>At feet</th><th>At anchor</th><th>Place?</th><th>Why</th></tr>");
            for (FeatureRow row : this.features) {
                sb.append("<tr><td>").append(CaveDebugReport.escape(row.featureId())).append("</td>");
                sb.append("<td class=\"").append(CaveDebugReport.css(row.filterStatus())).append("\">").append(CaveDebugReport.escape(row.filterStatus())).append("</td>");
                sb.append("<td class=\"").append(CaveDebugReport.css(row.chamberStatus())).append("\">").append(CaveDebugReport.escape(row.chamberStatus())).append("</td>");
                sb.append("<td class=\"").append(CaveDebugReport.css(row.feetStatus())).append("\">").append(CaveDebugReport.escape(row.feetStatus())).append("</td>");
                sb.append("<td class=\"").append(CaveDebugReport.css(row.anchorStatus())).append("\">").append(CaveDebugReport.escape(row.anchorStatus())).append("</td>");
                sb.append("<td class=\"").append(CaveDebugReport.css(row.placeAtAnchor())).append("\">").append(CaveDebugReport.escape(row.placeAtAnchor())).append("</td>");
                sb.append("<td>").append(CaveDebugReport.escape(row.reason())).append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    public static Path save(ServerLevel level, BlockPos pos, CaveDebugReport report) throws IOException {
        Path dir = level.getServer().getServerDirectory().toPath().resolve("config").resolve("NewTerraForged").resolve("debug");
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String base = String.format(Locale.ROOT, "cave-%d_%d_%d-%s", pos.getX(), pos.getY(), pos.getZ(), stamp);
        Path txt = dir.resolve(base + ".txt");
        Path html = dir.resolve(base + ".html");
        Files.writeString(txt, report.toPlainText(), StandardCharsets.UTF_8);
        Files.writeString(html, report.toHtml(), StandardCharsets.UTF_8);
        return txt;
    }

    private static String pad(String value, int width) {
        if (value.length() >= width) {
            return value.substring(0, width - 1) + "…";
        }
        return String.format(Locale.ROOT, "%-" + width + "s", value);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String css(String status) {
        if (status == null) {
            return "warn";
        }
        String lower = status.toLowerCase(Locale.ROOT);
        if ("allowed".equalsIgnoreCase(status) || "yes".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status) || lower.startsWith("likely")) {
            return "ok";
        }
        if ("blocked".equalsIgnoreCase(status) || "no".equalsIgnoreCase(status) || lower.startsWith("skip")) {
            return "bad";
        }
        return "warn";
    }

    public record FeatureRow(String featureId, String filterStatus, String chamberStatus, String feetStatus, String anchorStatus, String placeAtAnchor, String reason) {
    }
}
