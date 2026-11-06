package com.example.employee.config;

import com.example.employee.domain.Employee;
import com.example.employee.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds two sample employees on startup so the UI has data to show.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedEmployees(EmployeeRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(new Employee("张伟", "zhang.wei@example.com", "研发部", "后端工程师"));
            repository.save(new Employee("李娜", "li.na@example.com", "产品部", "产品经理"));
        };
    }
}
