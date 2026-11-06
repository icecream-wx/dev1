package com.example.employee.service;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("email 已被占用：" + email);
    }
}
