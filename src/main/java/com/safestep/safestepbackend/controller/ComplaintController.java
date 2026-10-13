package com.safestep.safestepbackend.controller;

import com.safestep.safestepbackend.model.Complaint;
import com.safestep.safestepbackend.repository.ComplaintRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    private static final Path UPLOAD_DIR = Paths.get("evidence-uploads");
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private final ComplaintRepository repository;

    public ComplaintController(ComplaintRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Complaint submit(@RequestBody Complaint complaint) {
        String id = "SS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        complaint.setComplaintId(id);
        complaint.setStatus("SUBMITTED");
        return repository.save(complaint);
    }

    @GetMapping
    public List<Complaint> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Complaint> updateStatus(@PathVariable String id,
                                                  @RequestBody Map<String, String> body) {
        return repository.findById(id)
                .map(c -> {
                    c.setStatus(body.get("status"));
                    return ResponseEntity.ok(repository.save(c));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<?> uploadEvidence(@PathVariable String id,
                                            @RequestParam("file") MultipartFile file) throws IOException {
        Optional<Complaint> found = repository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Complaint c = found.get();
        if (c.getEvidencePath() != null) {
            return ResponseEntity.status(409)
                    .body(Map.of("message", "Evidence was already uploaded for this complaint."));
        }
        String type = file.getContentType();
        if (file.isEmpty() || type == null || !ALLOWED_TYPES.contains(type)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only JPG, PNG or PDF files are allowed."));
        }
        String ext = type.equals("image/png") ? ".png"
                : type.equals("application/pdf") ? ".pdf" : ".jpg";
        Files.createDirectories(UPLOAD_DIR);
        String fileName = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(fileName),
                StandardCopyOption.REPLACE_EXISTING);
        c.setEvidencePath(fileName);
        repository.save(c);
        return ResponseEntity.ok(Map.of("message", "Evidence uploaded"));
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<Resource> getEvidence(@PathVariable String id) {
        Optional<Complaint> found = repository.findById(id);
        if (found.isEmpty() || found.get().getEvidencePath() == null) {
            return ResponseEntity.notFound().build();
        }
        Path file = UPLOAD_DIR.resolve(found.get().getEvidencePath()).normalize();
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        String name = file.getFileName().toString();
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG
                : name.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(file));
    }
}