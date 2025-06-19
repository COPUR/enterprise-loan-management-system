package com.bank.loanmanagement.customermanagement.infrastructure;

import com.bank.loanmanagement.customermanagement.domain.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Customer> CUSTOMER_ROW_MAPPER = new RowMapper<Customer>() {
        @Override
        public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Customer customer = new Customer();
            customer.setId(rs.getLong("id"));
            customer.setCustomerNumber(rs.getString("customer_number"));
            customer.setFirstName(rs.getString("first_name"));
            customer.setLastName(rs.getString("last_name"));
            customer.setEmail(rs.getString("email"));
            customer.setPhoneNumber(rs.getString("phone_number"));
            customer.setDateOfBirth(rs.getDate("date_of_birth") != null ? 
                rs.getDate("date_of_birth").toLocalDate() : null);
            customer.setSsn(rs.getString("ssn"));
            customer.setCreditScore(rs.getObject("credit_score", Integer.class));
            customer.setAnnualIncome(rs.getBigDecimal("annual_income"));
            customer.setEmploymentStatus(rs.getString("employment_status"));
            customer.setAddressLine1(rs.getString("address_line1"));
            customer.setAddressLine2(rs.getString("address_line2"));
            customer.setCity(rs.getString("city"));
            customer.setState(rs.getString("state"));
            customer.setZipCode(rs.getString("zip_code"));
            customer.setCountry(rs.getString("country"));
            customer.setStatus(rs.getString("status"));
            customer.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
            customer.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
            customer.setVersion(rs.getInt("version"));
            return customer;
        }
    };

    public List<Customer> findAll() {
        String sql = "SELECT * FROM customer_management.customers ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER);
    }

    public Optional<Customer> findById(Long id) {
        String sql = "SELECT * FROM customer_management.customers WHERE id = ?";
        List<Customer> customers = jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER, id);
        return customers.isEmpty() ? Optional.empty() : Optional.of(customers.get(0));
    }

    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        String sql = "SELECT * FROM customer_management.customers WHERE customer_number = ?";
        List<Customer> customers = jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER, customerNumber);
        return customers.isEmpty() ? Optional.empty() : Optional.of(customers.get(0));
    }

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            return insert(customer);
        } else {
            return update(customer);
        }
    }

    private Customer insert(Customer customer) {
        String sql = """
            INSERT INTO customer_management.customers (
                customer_number, first_name, last_name, email, phone_number,
                date_of_birth, ssn, credit_score, annual_income, employment_status,
                address_line1, address_line2, city, state, zip_code, country, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at, version
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            customer.setId(rs.getLong("id"));
            customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            customer.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            customer.setVersion(rs.getInt("version"));
            return customer;
        }, customer.getCustomerNumber(), customer.getFirstName(), customer.getLastName(),
           customer.getEmail(), customer.getPhoneNumber(), customer.getDateOfBirth(),
           customer.getSsn(), customer.getCreditScore(), customer.getAnnualIncome(),
           customer.getEmploymentStatus(), customer.getAddressLine1(), customer.getAddressLine2(),
           customer.getCity(), customer.getState(), customer.getZipCode(), 
           customer.getCountry(), customer.getStatus());
    }

    private Customer update(Customer customer) {
        String sql = """
            UPDATE customer_management.customers SET
                first_name = ?, last_name = ?, email = ?, phone_number = ?,
                date_of_birth = ?, credit_score = ?, annual_income = ?, employment_status = ?,
                address_line1 = ?, address_line2 = ?, city = ?, state = ?, zip_code = ?,
                country = ?, status = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1
            WHERE id = ? AND version = ?
            RETURNING updated_at, version
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            customer.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            customer.setVersion(rs.getInt("version"));
            return customer;
        }, customer.getFirstName(), customer.getLastName(), customer.getEmail(),
           customer.getPhoneNumber(), customer.getDateOfBirth(), customer.getCreditScore(),
           customer.getAnnualIncome(), customer.getEmploymentStatus(), customer.getAddressLine1(),
           customer.getAddressLine2(), customer.getCity(), customer.getState(),
           customer.getZipCode(), customer.getCountry(), customer.getStatus(),
           customer.getId(), customer.getVersion());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM customer_management.customers WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM customer_management.customers";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}