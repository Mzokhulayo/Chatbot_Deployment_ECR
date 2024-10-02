package com.example.chatbot.service;

import com.example.chatbot.model.Customer;
import com.example.chatbot.model.Otp;
import com.example.chatbot.repository.CustomerRepository;
import com.example.chatbot.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;



@Service
public class CustomerService {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Method to hash OTP before saving it to the database
    public String hashOtp(String otp) {
        return passwordEncoder.encode(otp);
    }


    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // Generate and store OTP in the database
    public void sendOtp(String phoneNumber) {

        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(5);

        String otp = generateOtp();

        // Hash the OTP before saving
        String hashedOtp = hashOtp(otp);
        Otp otpRecord = new Otp();
        otpRecord.setPhoneNumber(phoneNumber);
        otpRecord.setOtp(hashedOtp);
        otpRecord.setExpirationTime(expirationTime);
        otpRepository.saveOtp(otpRecord);
        System.out.println("Sending OTP" + hashedOtp + " to phone number: " + phoneNumber);
        // Here, integrate with an actual SMS API to send OTP
        System.out.println("Sending unhashed OTP: " + otp + " to phone number: " + phoneNumber);

    }


 //    Validating OTP and checking for Correctness

    public boolean validateOtp(String phoneNumber, String otp) {
        Optional<Otp> otpRecord = otpRepository.findByPhoneNumber(phoneNumber);
        if (otpRecord.isPresent()) {
            Otp storedOtp = otpRecord.get();
            System.out.println("Stored OTP (hashed): " + storedOtp.getOtp());
            System.out.println("Provided OTP (unhashed): " + otp);
            if (passwordEncoder.matches(otp, storedOtp.getOtp()) && LocalDateTime.now().isBefore(storedOtp.getExpirationTime())) {
                // Persist the OTP verified state in the database
                updateRegistrationState(phoneNumber, true, null);  // OTP verified, no registration data yet
                return true;
            }
        }
        return false;
    }


    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(100000);
        return String.valueOf(otp);
    }


    public Optional<Customer> findCustomerByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<Otp> findOtpByPhoneNumber(String phoneNumber) {
        return otpRepository.findByPhoneNumber(phoneNumber);
    }

    public Map<String, Object> getRegistrationState(String phoneNumber) {
        try {
            String sql = "SELECT otp_verified, registration_data FROM registration_state WHERE phone_number = ?";
            return jdbcTemplate.queryForMap(sql, phoneNumber);
        } catch (Exception e) {
            // Return null if no registration state is found (could throw an exception if there's no match)
            return null;
        }
    }


    public void updateRegistrationState(String phoneNumber, boolean otpVerified, String registrationData) {
        String sql = "INSERT INTO registration_state (phone_number, otp_verified, registration_data) " +
                "VALUES (?, ?, ?) ON CONFLICT (phone_number) DO UPDATE SET otp_verified = ?, registration_data = ?, updated_at = ?";

        jdbcTemplate.update(sql, phoneNumber, otpVerified, registrationData, otpVerified, registrationData, LocalDateTime.now());
    }
    public void saveRegistrationData(String phoneNumber, Customer newCustomer) {
        String registrationData = newCustomer.getName() + ", " + newCustomer.getSurname() + ", " + newCustomer.getEmail() + ", " + newCustomer.getAddress();
        updateRegistrationState(phoneNumber, true, registrationData); // Persist registration data and OTP verified state
    }
    public Customer registerCustomer(Customer customer) {
        customer.setCreatedAt(LocalDateTime.now());
        // Persist customer in the CustomerRepository
        customerRepository.save(customer);
        // Optionally remove the registration state if registration is complete
        removeRegistrationState(customer.getPhoneNumber());
        return customer;
    }

    public void removeRegistrationState(String phoneNumber) {
        String sql = "DELETE FROM registration_state WHERE phone_number = ?";
        jdbcTemplate.update(sql, phoneNumber);
    }

}

