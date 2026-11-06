package com.example.employee.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating and updating an employee.
 */
public record EmployeeRequest(
        @NotBlank(message = "name 不能为空")
        @Size(max = 100, message = "name 长度不能超过 100")
        String name,

        @NotBlank(message = "email 不能为空")
        @Email(message = "email 格式不正确")
        @Size(max = 200, message = "email 长度不能超过 200")
        String email,

        @Size(max = 100, message = "department 长度不能超过 100")
        String department,

        @Size(max = 100, message = "position 长度不能超过 100")
        String position) {
}
