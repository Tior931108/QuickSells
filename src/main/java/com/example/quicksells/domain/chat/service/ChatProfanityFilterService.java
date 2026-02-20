package com.example.quicksells.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 비속어 필터링 서비스
 */
@Slf4j
@Service
public class ChatProfanityFilterService {

    private final Set<String> profanityWords;
    private final Pattern[] profanityPatterns;

    public ChatProfanityFilterService() {
        this.profanityWords = new HashSet<>();

        // 파일에서 비속어 목록 로드
        loadProfanityWordsFromFile();

        // 정규표현식 패턴 컴파일
        this.profanityPatterns = compileProfanityPatterns();
    }

    private void loadProfanityWordsFromFile() {
        try {

            // 욕설 파일 리소스
            ClassPathResource resource = new ClassPathResource("profanity-words.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty() && !word.startsWith("#")) {  // # 주석 처리
                        profanityWords.add(word);
                    }
                }
            }

        } catch (IOException e) {
            log.error("비속어 파일 로드 실패: {}", e.getMessage());
        }
    }

    /**
     * 정규표현식 패턴 컴파일
     */
    private Pattern[] compileProfanityPatterns() {
        return profanityWords.stream()
                .map(word -> {
                    String regex = word.chars()
                            .mapToObj(c -> String.valueOf((char) c) + "[\\s]*")
                            .collect(Collectors.joining());
                    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                })
                .toArray(Pattern[]::new);
    }

    /**
     * 메인 메서드: 메시지 필터링
     *
     * @param message 원본 메시지
     * @return 필터링된 메시지
     */
    public String filterProfanity(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }

        String filtered = message;

        // 1. 완전 일치 필터링
        for (String word : profanityWords) {
            if (filtered.toLowerCase().contains(word.toLowerCase())) {
                filtered = filtered.replaceAll("(?i)" + Pattern.quote(word), getMask(word.length()));
                log.warn("비속어 감지 및 필터링: '{}'", word);
            }
        }

        // 2. 정규표현식 패턴 필터링
        for (Pattern pattern : profanityPatterns) {
            if (pattern.matcher(filtered).find()) {
                filtered = pattern.matcher(filtered).replaceAll("***");
                log.warn("비속어 패턴 감지 및 필터링: {}", pattern.pattern());
            }
        }

        return filtered;
    }


    /**
     * 마스킹 문자열 생성 (비속어 길이만큼 *)
     */
    private String getMask(int length) {
        return "*".repeat(Math.max(1, length));
    }

}
