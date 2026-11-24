package com.example.masrouf1;

public enum AppRole {
    PARENT("parent"), USER("user"), CHILD("child");

    private final String value;

    AppRole(String value) { this.value = value; }

    public String value() { return value; }

    public static AppRole from(String value) {
        if("admin".equalsIgnoreCase(value)) return PARENT;
        for (AppRole role : values()) if (role.value.equalsIgnoreCase(value)) return role;
        return USER;
    }
}
