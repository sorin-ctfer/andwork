package com.example.movinghacker;

public class ModuleItem {
    private final String id;
    private final String name;
    private final int iconResId;
    private final String description;

    public ModuleItem(String id, String name, int iconResId, String description) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getDescription() {
        return description;
    }
}
