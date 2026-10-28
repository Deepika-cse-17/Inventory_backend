package com.warehouse.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name cannot exceed 120 characters")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,
        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message cannot exceed 4000 characters")
        String message) {
}