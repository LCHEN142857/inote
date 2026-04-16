package com.inote.security;

import com.inote.model.entity.User;

public final class CurrentUserHolder {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(User user) {
        CURRENT_USER.set(user);
    }

    public static User getRequired() {
        User user = CURRENT_USER.get();
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
