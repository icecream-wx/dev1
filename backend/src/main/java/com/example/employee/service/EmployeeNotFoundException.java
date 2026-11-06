package com.example.employee.service;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("员工不存在：id=" + id);
    }
}
