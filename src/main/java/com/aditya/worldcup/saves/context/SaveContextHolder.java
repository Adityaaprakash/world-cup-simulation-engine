package com.aditya.worldcup.saves.context;

public class SaveContextHolder {
    private static final ThreadLocal<Long> managerIdHolder = new ThreadLocal<>();

    public static void setManagerId(Long managerId) {
        managerIdHolder.set(managerId);
    }

    public static Long getManagerId() {
        return managerIdHolder.get();
    }

    public static void clear() {
        managerIdHolder.remove();
    }
}
