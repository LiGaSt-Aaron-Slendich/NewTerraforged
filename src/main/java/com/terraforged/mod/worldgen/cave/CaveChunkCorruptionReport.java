package com.terraforged.mod.worldgen.cave;

import java.util.EnumSet;
import java.util.Locale;

public final class CaveChunkCorruptionReport {
    public enum Issue {
        UNDERGROUND_FEATURES("Underground features failure"),
        NOISE("Noise failure");

        private final String label;

        Issue(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    private final EnumSet<Issue> issues;

    private CaveChunkCorruptionReport(EnumSet<Issue> issues) {
        this.issues = issues;
    }

    public static CaveChunkCorruptionReport clean() {
        return new CaveChunkCorruptionReport(EnumSet.noneOf(Issue.class));
    }

    public static CaveChunkCorruptionReport of(Issue... issues) {
        EnumSet<Issue> set = EnumSet.noneOf(Issue.class);
        for (Issue issue : issues) {
            if (issue != null) {
                set.add(issue);
            }
        }
        return new CaveChunkCorruptionReport(set);
    }

    public void add(Issue issue) {
        if (issue != null) {
            this.issues.add(issue);
        }
    }

    public boolean corrupted() {
        return !this.issues.isEmpty();
    }

    public EnumSet<Issue> issues() {
        return EnumSet.copyOf(this.issues);
    }

    public String detail() {
        if (this.issues.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Issue issue : this.issues) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(issue.label());
            first = false;
        }
        return builder.toString();
    }

    public CaveChunkCorruptionReport merge(CaveChunkCorruptionReport other) {
        if (other == null || !other.corrupted()) {
            return this;
        }
        EnumSet<Issue> merged = EnumSet.copyOf(this.issues);
        merged.addAll(other.issues);
        return new CaveChunkCorruptionReport(merged);
    }
}
