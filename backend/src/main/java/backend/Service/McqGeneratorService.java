package backend.Service;

import backend.Model.Question;
import backend.Repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates MCQs from text (mock AI - can be replaced with real AI API).
 */
@Service
public class McqGeneratorService {

    @Autowired
    private QuestionRepository questionRepository;

    private static final Pattern SENTENCE = Pattern.compile("[^.!?]+[.!?]");
        private static final Pattern TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9-]{2,}");
    private static final int REQUIRED_MIN_QUESTIONS = 20;
    private static final int MIN_TEXT_LENGTH = 20;
    private static final int MIN_SENTENCE_LENGTH = 10;
        private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "that", "with", "this", "from", "have", "are", "was", "were", "been",
            "into", "their", "about", "which", "when", "where", "your", "you", "they", "them", "there",
            "than", "then", "will", "would", "could", "should", "also", "using", "used", "use", "such",
            "many", "most", "more", "less", "some", "each", "only", "very", "between", "after", "before",
            "note", "notes", "document", "study", "students", "student"
        );

    /**
     * Extract sentences from raw text for use in questions.
     */
    public List<String> extractSentences(String text) {
        if (text == null || text.trim().length() < MIN_TEXT_LENGTH)
            return Collections.emptyList();
        String cleaned = text.replaceAll("\\s+", " ").trim();
        List<String> list = new ArrayList<>();
        java.util.regex.Matcher m = SENTENCE.matcher(cleaned);
        while (m.find()) list.add(m.group().trim());
        return list;
    }

    /**
     * Generate MCQ questions from text and save to DB. Returns generated questions.
     */
    public List<Question> generateAndSave(Long subjectId, Long documentId, String text) {
        List<String> sentences = extractSentences(text).stream()
                .filter(s -> s.length() >= MIN_SENTENCE_LENGTH)
                .collect(Collectors.toList());
        if (sentences.isEmpty())
            return Collections.emptyList();

        questionRepository.deleteByDocumentId(documentId);

        List<String> keywordPool = buildKeywordPool(text);
        Random random = new Random((documentId != null ? documentId : 0L) + text.length());

        List<Question> questions = new ArrayList<>();
        Set<String> seenQuestions = new HashSet<>();
        for (String sentence : sentences) {
            Question q = buildSentenceQuestion(subjectId, documentId, sentence, keywordPool, random);
            if (q == null || !seenQuestions.add(q.getQuestionText())) continue;
            questions.add(questionRepository.save(q));
        }

        if (questions.size() < REQUIRED_MIN_QUESTIONS) {
            addFallbackQuestions(subjectId, documentId, text, questions, keywordPool, random, seenQuestions);
        }

        return questions;
    }

    private void addFallbackQuestions(
            Long subjectId,
            Long documentId,
            String text,
            List<Question> questions,
            List<String> keywordPool,
            Random random,
            Set<String> seenQuestions
    ) {
        String[] templates = new String[] {
                "Which term is most central to this topic summary?",
                "Which keyword appears as an important concept in the notes?",
                "Which term best fits the study material?",
                "Which concept is emphasized in this document?",
                "Which keyword is most likely part of the core content?"
        };

        int i = 0;
        while (questions.size() < REQUIRED_MIN_QUESTIONS) {
            String template = templates[i % templates.length];
            String hint = truncate(text, 140);
            String questionText = template + " Context: \"" + hint + "\"";
            if (seenQuestions.contains(questionText)) {
                i++;
                continue;
            }

            List<String> options = buildOptionSet(
                    pickBestKeyword(keywordPool, random),
                    keywordPool,
                    random
            );
            if (options.size() < 4) {
                i++;
                continue;
            }

            Question q = buildQuestionWithOptions(subjectId, documentId, questionText, options, options.get(0), random);
            seenQuestions.add(questionText);
            questions.add(questionRepository.save(q));
            i++;
        }
    }

    private Question buildSentenceQuestion(
            Long subjectId,
            Long documentId,
            String sentence,
            List<String> keywordPool,
            Random random
    ) {
        String keyword = pickKeywordFromSentence(sentence, keywordPool);
        if (keyword == null) {
            return null;
        }

        // Cycle through different question types for variety
        int questionType = random.nextInt(5);
        
        return switch (questionType) {
            case 0 -> buildClozeFillQuestion(subjectId, documentId, sentence, keyword, keywordPool, random);
            case 1 -> buildDefinitionQuestion(subjectId, documentId, keyword, keywordPool, random);
            case 2 -> buildConceptualQuestion(subjectId, documentId, keyword, keywordPool, random);
            case 3 -> buildRelationshipQuestion(subjectId, documentId, sentence, keyword, keywordPool, random);
            default -> buildDescriptiveQuestion(subjectId, documentId, sentence, keyword, keywordPool, random);
        };
    }

    /**
     * Type 1: Fill-in-the-blank cloze question
     */
    private Question buildClozeFillQuestion(
            Long subjectId,
            Long documentId,
            String sentence,
            String keyword,
            List<String> keywordPool,
            Random random
    ) {
        String masked = sentence.replaceFirst("(?i)\\b" + Pattern.quote(keyword) + "\\b", "_____");
        String questionText = "Complete the blank from your notes: \"" + truncate(masked, 160) + "\"";

        List<String> options = buildOptionSet(keyword, keywordPool, random);
        if (options.size() < 4) {
            return null;
        }
        return buildQuestionWithOptions(subjectId, documentId, questionText, options, keyword, random);
    }

    /**
     * Type 2: Definition/meaning question
     */
    private Question buildDefinitionQuestion(
            Long subjectId,
            Long documentId,
            String keyword,
            List<String> keywordPool,
            Random random
    ) {
        String[] templates = new String[]{
            "In the context of your notes, what does '%s' refer to?",
            "Which best describes the meaning of '%s' based on the study material?",
            "What is the primary definition of '%s' in your notes?",
            "Which statement best explains '%s'?"
        };
        
        String template = templates[random.nextInt(templates.length)];
        String questionText = String.format(template, keyword);

        List<String> options = buildOptionSet(keyword, keywordPool, random);
        if (options.size() < 4) {
            return null;
        }
        return buildQuestionWithOptions(subjectId, documentId, questionText, options, keyword, random);
    }

    /**
     * Type 3: Conceptual question - why/importance
     */
    private Question buildConceptualQuestion(
            Long subjectId,
            Long documentId,
            String keyword,
            List<String> keywordPool,
            Random random
    ) {
        String[] templates = new String[]{
            "Why is '%s' important in this study material?",
            "What is the significance of '%s' based on your notes?",
            "Which option best explains the role of '%s'?",
            "Why would '%s' be emphasized in these notes?"
        };
        
        String template = templates[random.nextInt(templates.length)];
        String questionText = String.format(template, keyword);

        List<String> options = buildOptionSet(keyword, keywordPool, random);
        if (options.size() < 4) {
            return null;
        }
        return buildQuestionWithOptions(subjectId, documentId, questionText, options, keyword, random);
    }

    /**
     * Type 4: Relationship question - what relates to/connects with
     */
    private Question buildRelationshipQuestion(
            Long subjectId,
            Long documentId,
            String sentence,
            String keyword,
            List<String> keywordPool,
            Random random
    ) {
        String[] templates = new String[]{
            "Which concept is most closely related to '%s'?",
            "What is most often associated with '%s' based on the notes?",
            "Which of these connects best with '%s'?",
            "In your study material, '%s' is most related to:"
        };
        
        String template = templates[random.nextInt(templates.length)];
        String questionText = String.format(template, keyword);

        // Pick related keywords from pool
        List<String> relatedOptions = new ArrayList<>(keywordPool);
        Collections.shuffle(relatedOptions, random);
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add(keyword);
        for (String opt : relatedOptions) {
            if (!opt.equalsIgnoreCase(keyword) && options.size() < 4) {
                options.add(opt);
            }
        }

        if (options.size() < 4) {
            options.add("abstract");
            options.add("practical");
            options.add("theoretical");
        }

        List<String> optionsList = new ArrayList<>(options).subList(0, Math.min(4, options.size()));
        if (optionsList.size() < 4) return null;

        return buildQuestionWithOptions(subjectId, documentId, questionText, optionsList, keyword, random);
    }

    /**
     * Type 5: Descriptive question - characteristics/features
     */
    private Question buildDescriptiveQuestion(
            Long subjectId,
            Long documentId,
            String sentence,
            String keyword,
            List<String> keywordPool,
            Random random
    ) {
        String[] templates = new String[]{
            "Which statement best describes '%s'?",
            "Which of the following is true about '%s'?",
            "Based on the material, '%s' can be characterized as:",
            "What feature is most associated with '%s'?"
        };
        
        String template = templates[random.nextInt(templates.length)];
        String questionText = String.format(template, keyword);

        List<String> options = buildOptionSet(keyword, keywordPool, random);
        if (options.size() < 4) {
            return null;
        }
        return buildQuestionWithOptions(subjectId, documentId, questionText, options, keyword, random);
    }

    private Question buildQuestionWithOptions(
            Long subjectId,
            Long documentId,
            String questionText,
            List<String> options,
            String correct,
            Random random
    ) {
        List<String> shuffled = new ArrayList<>(options);
        Collections.shuffle(shuffled, random);

        Question q = new Question();
        q.setSubjectId(subjectId);
        q.setDocumentId(documentId);
        q.setQuestionText(questionText);
        q.setOptionA(shuffled.get(0));
        q.setOptionB(shuffled.get(1));
        q.setOptionC(shuffled.get(2));
        q.setOptionD(shuffled.get(3));
        q.setCorrectAnswer(letterForCorrect(shuffled, correct));
        return q;
    }

    private String letterForCorrect(List<String> options, String correct) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(correct)) {
                return switch (i) {
                    case 0 -> "A";
                    case 1 -> "B";
                    case 2 -> "C";
                    default -> "D";
                };
            }
        }
        return "A";
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

    private String pickKeywordFromSentence(String sentence, List<String> keywordPool) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        for (String token : keywordPool) {
            if (lower.contains(token)) return token;
        }

        java.util.regex.Matcher matcher = TOKEN.matcher(sentence);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (token.length() >= 4 && !STOPWORDS.contains(token)) {
                return token;
            }
        }
        return null;
    }

    private String pickBestKeyword(List<String> keywordPool, Random random) {
        if (keywordPool.isEmpty()) return "core concept";
        int bound = Math.min(10, keywordPool.size());
        return keywordPool.get(random.nextInt(bound));
    }

    private List<String> buildOptionSet(String correct, List<String> keywordPool, Random random) {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add(correct);

        List<String> candidates = new ArrayList<>(keywordPool);
        Collections.shuffle(candidates, random);
        for (String candidate : candidates) {
            if (!candidate.equalsIgnoreCase(correct)) {
                options.add(candidate);
            }
            if (options.size() == 4) break;
        }

        if (options.size() < 4) {
            options.add("generalization");
            options.add("assumption");
            options.add("outlier");
        }

        return new ArrayList<>(options).subList(0, 4);
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
