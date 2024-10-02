package com.example.chatbot.repository;

import com.example.chatbot.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@Service
public class CustomerRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public Customer save(Customer customer) {
        // Generate a new UUID for the customer
        UUID customerId = UUID.randomUUID();
        customer.setId(customerId);
        customer.setCreatedAt(LocalDateTime.now()); // Set the current time

        String sql = "INSERT INTO customers (id, name, surname, phone_number, email, address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, customerId, customer.getName(), customer.getSurname(), customer.getPhoneNumber(), customer.getEmail(), customer.getAddress(), customer.getCreatedAt());

        return customer;  // Return the saved customer with ID set
    }


    public Optional<Customer> findByPhoneNumber(String phoneNumber) {
        String sql = "SELECT * FROM customers WHERE phone_number = ?";
        return jdbcTemplate.query(sql, new Object[]{phoneNumber}, new CustomerRowMapper())
                .stream().findFirst();
    }

    private static class CustomerRowMapper implements RowMapper<Customer> {
        @Override
        public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Customer customer = new Customer();
            customer.setId(UUID.fromString(rs.getString("id")));
            customer.setName(rs.getString("name"));
            customer.setSurname(rs.getString("surname"));
            customer.setPhoneNumber(rs.getString("phone_number"));
            customer.setEmail(rs.getString("email"));
            customer.setAddress(rs.getString("address"));
            customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return customer;
        }
    }
}
