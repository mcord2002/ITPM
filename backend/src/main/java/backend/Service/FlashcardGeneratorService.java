package backend.Service;

import backend.Model.Flashcard;
import backend.Repository.FlashcardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FlashcardGeneratorService {

    @Autowired
    private FlashcardRepository flashcardRepository;

    private static final Pattern SENTENCE = Pattern.compile("[^.!?]+[.!?]");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9-]{2,}");
    private static final int MIN_TEXT_LENGTH = 20;
    private static final int MIN_SENTENCE_LENGTH = 18;
    private static final int REQUIRED_MIN_FLASHCARDS = 10;
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "that", "with", "this", "from", "have", "are", "was", "were", "been",
            "into", "their", "about", "which", "when", "where", "your", "you", "they", "them", "there",
            "than", "then", "will", "would", "could", "should", "also", "using", "used", "use", "such",
            "many", "most", "more", "less", "some", "each", "only", "very", "between", "after", "before",
            "note", "notes", "document", "study", "students", "student"
    );

    public List<Flashcard> generateAndSave(Long subjectId, Long documentId, String text) {
        List<String> points = extractPoints(text);
        if (points.isEmpty()) return Collections.emptyList();

        flashcardRepository.deleteByDocumentId(documentId);

        List<String> keywordPool = buildKeywordPool(text);

        List<Flashcard> cards = new ArrayList<>();
        LinkedHashSet<String> seenFronts = new LinkedHashSet<>();
        for (String point : points) {
            String keyword = pickKeyword(point, keywordPool);
            String front = keyword == null
                    ? "Explain this idea in simple terms."
                    : "What does \"" + keyword + "\" refer to in your notes?";

            if (!seenFronts.add(front)) {
                front = "Complete and explain: \"" + truncate(maskSentence(point, keyword), 120) + "\"";
            }

            Flashcard card = new Flashcard();
            card.setSubjectId(subjectId);
            card.setDocumentId(documentId);
            card.setFrontText(front);
            card.setBackText(truncate(point, 220));
            cards.add(flashcardRepository.save(card));
        }

        int index = 0;
        while (cards.size() < REQUIRED_MIN_FLASHCARDS) {
            String point = points.get(index % points.size());
            String keyword = pickKeyword(point, keywordPool);
            Flashcard card = new Flashcard();
            card.setSubjectId(subjectId);
            card.setDocumentId(documentId);
            card.setFrontText(keyword == null
                    ? "Summarize this concept in one line."
                    : "Why is \"" + keyword + "\" important here?");
            card.setBackText(truncate(point, 220));
            cards.add(flashcardRepository.save(card));
            index++;
        }

        return cards;
    }

    private List<String> extractPoints(String text) {
        if (text == null || text.trim().length() < MIN_TEXT_LENGTH) return Collections.emptyList();
        String cleaned = text.replaceAll("\\s+", " ").trim();
        java.util.regex.Matcher matcher = SENTENCE.matcher(cleaned);
        List<String> sentences = new ArrayList<>();
        while (matcher.find()) {
            sentences.add(matcher.group().trim());
        }

        if (sentences.isEmpty()) {
            return List.of(cleaned);
        }

        return sentences.stream()
                .filter(s -> s.length() >= MIN_SENTENCE_LENGTH)
                .limit(20)
                .collect(Collectors.toList());
    }

    private String maskSentence(String sentence, String keyword) {
        if (keyword == null || keyword.isBlank()) return sentence;
        return sentence.replaceFirst("(?i)\\b" + Pattern.quote(keyword) + "\\b", "_____");
    }

    private List<String> buildKeywordPool(String text) {
        Map<String, Integer> counts = new HashMap<>();
        java.util.regex.Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (token.length() < 4 || STOPWORDS.contains(token)) continue;
            counts.merge(token, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(80)
                .collect(Collectors.toList());
    }

    private String pickKeyword(String sentence, List<String> keywordPool) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        for (String token : keywordPool) {
            if (lower.contains(token)) return token;
        }

        java.util.regex.Matcher matcher = TOKEN.matcher(sentence);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (token.length() >= 4 && !STOPWORDS.contains(token)) return token;
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }
}