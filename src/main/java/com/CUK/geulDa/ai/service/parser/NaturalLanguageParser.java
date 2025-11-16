package com.CUK.geulDa.ai.service.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NaturalLanguageParser {

    public ParsedRequest parseUserRequest(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedRequest("", List.of(), 4);
        }

        String cleaned = input;
        List<String> excludeCategories = new ArrayList<>();
        int placeCount = 4;

        String[] excludePatterns = {"제외하고", "빼고", "제외", "뺴고", "말고"};
        for (String pattern : excludePatterns) {
            if (cleaned.contains(pattern)) {
                int idx = cleaned.indexOf(pattern);
                String before = cleaned.substring(0, idx).trim();

                String[] words = before.split("\\s+");
                if (words.length > 0) {
                    String category = words[words.length - 1]
                            .replace("은", "").replace("는", "")
                            .replace("을", "").replace("를", "").trim();
                    if (!category.isEmpty()) {
                        excludeCategories.add(category);
                    }
                }
                cleaned = cleaned.substring(idx + pattern.length()).trim();
            }
        }

        if (cleaned.contains("많이")) {
            placeCount = 10;
            cleaned = cleaned.replace("많이", "").trim();
        } else if (cleaned.matches(".*\\d+개.*")) {
            Pattern pattern = Pattern.compile("(\\d+)개");
            Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                placeCount = Integer.parseInt(matcher.group(1));
                cleaned = cleaned.replaceAll("\\d+개(만)?", "").trim();
            }
        }

        placeCount = Math.max(1, Math.min(20, placeCount));

        cleaned = cleaned.replace("추천해줘", "")
                .replace("알려줘", "")
                .replace("보여줘", "")
                .replace("찾아줘", "").trim();

        return new ParsedRequest(cleaned, excludeCategories, placeCount);
    }

    public record ParsedRequest(
            String cleanedMustVisitPlace,
            List<String> excludeCategories,
            int placeCount
    ) {}
}
