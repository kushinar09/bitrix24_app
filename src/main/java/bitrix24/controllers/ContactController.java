package bitrix24.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bitrix24.entities.ContactResponse;
import bitrix24.services.ContactService;

@RestController
@RequestMapping("/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public List<ContactResponse> getAll() throws IOException {
        return contactService.getAllContacts();
    }

    @PostMapping("/add")
    public ResponseEntity<String> create(@RequestBody ContactResponse dto) {
        try {
            contactService.createContact(dto);
            return ResponseEntity.ok("Contact created");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error creating contact: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody UpdateContactRequest request) {
        try {
            contactService.updateContact(request.contact, request.deletedEmails, request.deletedPhones, request.deleteWebsites);
            return ResponseEntity.ok("Contact updated");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Invalid contact data: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok("Contact deleted");
    }
}

class UpdateContactRequest {
    public ContactResponse contact;
    public List<Integer> deletedEmails;
    public List<Integer> deletedPhones;
    public List<Integer> deleteWebsites;
}
