package backend.Controller;

import backend.Model.KnowledgeEntry;
import backend.Repository.KnowledgeEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class KnowledgeEntryController {

    @Autowired
    private KnowledgeEntryRepository knowledgeRepository;

    @GetMapping
    public List<KnowledgeEntry> getAll(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank())
            return knowledgeRepository.findByCategoryOrderByTitleAsc(category.trim());
        return knowledgeRepository.findAllByOrderByCategoryAscTitleAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeEntry> getById(@PathVariable Long id) {
        return knowledgeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public KnowledgeEntry create(@RequestBody KnowledgeEntry entry) {
        if (entry.getCreatedAt() == null) entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        return knowledgeRepository.save(entry);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeEntry> update(@PathVariable Long id, @RequestBody KnowledgeEntry body) {
        return knowledgeRepository.findById(id)
                .map(k -> {
                    if (body.getTitle() != null) k.setTitle(body.getTitle());
                    if (body.getCategory() != null) k.setCategory(body.getCategory());
                    if (body.getContent() != null) k.setContent(body.getContent());
                    k.setUpdatedAt(LocalDateTime.now());
                    return ResponseEntity.ok(knowledgeRepository.save(k));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!knowledgeRepository.existsById(id))
            return ResponseEntity.notFound().build();
        knowledgeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
