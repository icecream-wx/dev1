package com.example.employee.web.dto;

import com.example.employee.domain.Employee;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String department,
        String position) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getPosition());
    }
}
