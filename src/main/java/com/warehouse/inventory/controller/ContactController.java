package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.ContactMessageRequest;
import com.warehouse.inventory.model.ContactMessage;
import com.warehouse.inventory.repository.ContactMessageRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactMessageRepository contactMessageRepository;

    public ContactController(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody ContactMessageRequest request) {
        contactMessageRepository.save(
            new ContactMessage(request.name().trim(), request.email().trim(), request.message().trim()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
            "message", "Your message was sent"));
    }

    @GetMapping("/messages")
    public Map<String, Object> messages() {
        List<ContactMessage> messages = contactMessageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return Map.of("success", true, "messages", messages, "count", messages.size());
    }
}