package com.inote.security;

import com.inote.model.entity.User;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public User getCurrentUser() {
        return CurrentUserHolder.getRequired();
    }
}
