package com.CUK.geulDa.ai.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataTransformService {

    public String normalizeTransportation(String transportation) {
        return switch (transportation) {
            case "도보", "walk" -> "walk";
            case "대중교통", "transit" -> "transit";
            case "자동차", "car" -> "car";
            default -> {
                log.warn("알 수 없는 교통수단: {}, 기본값(transit) 사용", transportation);
                yield "transit";
            }
        };
    }

    public String normalizePurpose(String purpose) {
        return switch (purpose) {
            case "데이트", "dating" -> "dating";
            case "가족", "family" -> "family";
            case "친구", "friendship" -> "friendship";
            case "맛집", "맛집 탐방", "foodie" -> "foodie";
            default -> {
                log.warn("알 수 없는 여행 목적: {}, 원본값 사용", purpose);
                yield purpose;
            }
        };
    }

    public String translateTransportation(String transportation) {
        return switch (transportation) {
            case "walk", "도보" -> "도보";
            case "transit", "대중교통" -> "대중교통";
            case "car", "자동차" -> "자동차";
            default -> transportation;
        };
    }

    public String translatePurpose(String purpose) {
        return switch (purpose) {
            case "dating", "데이트" -> "데이트";
            case "family", "가족" -> "가족";
            case "friendship", "친구" -> "친구";
            case "foodie", "맛집", "맛집 탐방" -> "맛집 탐방";
            default -> purpose;
        };
    }

    public double getRadius(String transportation) {
        return switch (transportation) {
            case "walk" -> 1.0;
            case "transit" -> 3.0;
            case "car" -> 10.0;
            default -> 3.0;
        };
    }
}
