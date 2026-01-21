package com.example.sunxu_mall.context;

/**
 * @author sunxu
 */

public class AuditContextHolder {
    private static final ThreadLocal<AuditUser> CONTEXT = new ThreadLocal<>();

    public static void set(AuditUser auditUser) {
        CONTEXT.set(auditUser);
    }

    public static AuditUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

