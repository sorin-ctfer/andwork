package com.example.movinghacker;

public class TerminalOutputBuffer {
    private final int maxChars;
    private final StringBuilder buffer = new StringBuilder();

    public TerminalOutputBuffer(int maxChars) {
        this.maxChars = Math.max(1024, maxChars);
    }

    public synchronized void append(String text) {
        if (text == null || text.isEmpty()) return;
        buffer.append(text);
        trimIfNeeded();
    }

    public synchronized void clear() {
        buffer.setLength(0);
    }

    public synchronized String getText() {
        return buffer.toString();
    }

    private void trimIfNeeded() {
        int overflow = buffer.length() - maxChars;
        if (overflow <= 0) return;
        int trimTo = Math.min(buffer.length(), overflow + (maxChars / 10));
        buffer.delete(0, trimTo);
    }
}
