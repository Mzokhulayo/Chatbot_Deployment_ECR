package com.example.chatbot.controller;

import com.example.chatbot.DataTransfareObject.ChatbotRequest;
import com.example.chatbot.model.Customer;
import com.example.chatbot.service.CustomerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@RestController
@RequestMapping("/api")
@Validated
public class ChatbotController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/message")
    public Map<String, String> handleMessage(@Valid @RequestBody ChatbotRequest request) {
        String phone = request.getPhone();
        String message = request.getMessage();
        Map<String, String> response = new HashMap<>();

        // Step 1: Check if the user is already registered
        Optional<Customer> customerOptional = customerService.findCustomerByPhoneNumber(phone);

        if (customerOptional.isPresent()) {
            // If the user is already registered, handle their request as a returning customer
            Customer customer = customerOptional.get();
            response.put("message", "Welcome back, " + customer.getName() + "! You said: " + message + ". How can I assist you today?");
        } else {
            // Step 2: Check the user's registration state from the database
            Map<String, Object> registrationState = customerService.getRegistrationState(phone);

            if (registrationState != null) {
                boolean otpVerified = (Boolean) registrationState.get("otp_verified");
                String registrationData = (String) registrationState.get("registration_data");

                // Step 3: OTP not verified yet, ask for OTP input or resend OTP
                if (!otpVerified) {
                    if (customerService.validateOtp(phone, message)) {
                        // OTP verified, update the registration state
                        customerService.updateRegistrationState(phone, true, null);
                        response.put("message", "OTP verified! Please provide your details in the format: Name, Surname, Email, Physical Address.");
                    } else {
                        response.put("message", "Invalid OTP or OTP expired. Please enter the correct OTP.");
                    }
                }
                // Step 4: OTP verified but registration is incomplete, ask for registration details
                else if (registrationData == null) {
                    String[] userInformation = message.split(", ");
                    if (userInformation.length == 4) {
                        // Complete registration
                        Customer newCustomer = new Customer();
                        newCustomer.setName(userInformation[0]);
                        newCustomer.setSurname(userInformation[1]);
                        newCustomer.setEmail(userInformation[2]);
                        newCustomer.setAddress(userInformation[3]);
                        newCustomer.setPhoneNumber(phone);

                        // Call saveRegistrationData to persist the registration state
                        customerService.saveRegistrationData(phone, newCustomer);

                        // Register the customer
                        customerService.registerCustomer(newCustomer);
                        response.put("message", "Registration successful! Welcome to The CSD App.");
                    } else {
                        response.put("message", "Please provide your details in the correct format: Name, Surname, Email, Physical Address.");
                    }
                }
                // Step 5: Handle case where registration is partially complete
                else {
                    response.put("message", "We have your data partially saved. Please complete the registration.");
                }
            } else {
                // Step 6: No registration state found, start a new registration process by sending OTP
                customerService.sendOtp(phone);
                response.put("message", "Your phone number was not found. To register, please provide the OTP sent to your number.");
                customerService.updateRegistrationState(phone, false, null); // Start new registration, OTP not yet verified
            }
        }

        return response;
    }
}



