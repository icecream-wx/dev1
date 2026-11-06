package com.example.employee.service;

import com.example.employee.domain.Employee;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.web.dto.EmployeeRequest;
import com.example.employee.web.dto.EmployeeResponse;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    public List<EmployeeResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalize(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        Employee employee = new Employee(
                request.name().trim(), email, normalize(request.department()), normalize(request.position()));
        return EmployeeResponse.from(repository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        String email = normalize(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }

        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setDepartment(normalize(request.department()));
        employee.setPosition(normalize(request.position()));
        return EmployeeResponse.from(repository.save(employee));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
