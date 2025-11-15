package com.CUK.geulDa.domain.course.service;

import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public List<String> getTourPurposeTags(Course course) {
        if (course.getTourPurposeTags() == null || course.getTourPurposeTags().isBlank()) {
            return List.of();
        }
        return List.of(course.getTourPurposeTags().split(","));
    }

    public List<Course> findPlacesWithinRadius(double lat, double lon, double radius) {
        return courseRepository.findWithinRadius(lat, lon, radius);
    }

    public List<Course> findByKeyword(String keyword) {
        return courseRepository.findByNameContainingAndIsHiddenFalse(keyword);
    }

    public List<Course> filterByPurpose(List<Course> courses, String purpose) {
        // 영어 → 한글 매핑
        String koreanPurpose = switch (purpose) {
            case "dating" -> "데이트";
            case "family" -> "가족";
            case "friendship" -> "친구";
            case "foodie" -> "식도락";
            default -> purpose;
        };

        // 1. 목적에 맞는 장소 필터링
        List<Course> filtered = courses.stream()
                .filter(course -> {
                    List<String> tags = getTourPurposeTags(course);
                    return tags.contains(purpose) || tags.contains(koreanPurpose);
                })
                .toList();

        // 2. 카테고리별로 그룹화
        Map<String, List<Course>> byCategory = filtered.stream()
                .collect(Collectors.groupingBy(
                        course -> course.getCategory() != null ? course.getCategory() : "기타"
                ));

        // 3. 카테고리별 우선순위 (데이트 목적 기준)
        List<String> categoryPriority = "dating".equals(purpose) || "데이트".equals(koreanPurpose)
                ? List.of("자연", "문화시설", "카페", "음식점", "쇼핑", "기타")
                : List.of("문화시설", "자연", "음식점", "카페", "쇼핑", "기타");

        // 4. 카테고리별로 가중치 랜덤 샘플링 (각 카테고리에서 최대 3개씩)
        List<Course> balanced = new ArrayList<>();
        Random random = new Random();

        for (String category : categoryPriority) {
            List<Course> categoryCourses = byCategory.getOrDefault(category, List.of());

            // 인기도 기반 가중치 랜덤 샘플링
            List<Course> sampled = weightedRandomSample(categoryCourses, 3, random);
            balanced.addAll(sampled);
        }

        return balanced;
    }

    /**
     * 인기도 기반 가중치 랜덤 샘플링
     * 인기도가 높을수록 선택될 확률이 높지만, 매번 다른 결과 반환
     */
    private List<Course> weightedRandomSample(List<Course> courses, int count, Random random) {
        if (courses.isEmpty()) {
            return List.of();
        }

        if (courses.size() <= count) {
            return new ArrayList<>(courses);
        }

        // 가중치 계산 (인기도를 확률로 변환)
        List<WeightedCourse> weighted = courses.stream()
                .map(course -> {
                    int score = course.getPopularityScore() != null ? course.getPopularityScore() : 50;
                    // 인기도 40점 이하는 최소 가중치, 70점 이상은 높은 가중치
                    double weight = score < 40 ? 0.5 : (score >= 70 ? 3.0 : 1.0);
                    return new WeightedCourse(course, weight);
                })
                .toList();

        double totalWeight = weighted.stream().mapToDouble(w -> w.weight).sum();

        // 가중치 기반 랜덤 샘플링
        List<Course> result = new ArrayList<>();
        List<WeightedCourse> remaining = new ArrayList<>(weighted);

        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            double randomValue = random.nextDouble() * totalWeight;
            double cumulative = 0.0;

            WeightedCourse selected = null;
            for (WeightedCourse wp : remaining) {
                cumulative += wp.weight;
                if (randomValue <= cumulative) {
                    selected = wp;
                    break;
                }
            }

            if (selected == null) {
                selected = remaining.get(remaining.size() - 1);
            }

            result.add(selected.course);
            remaining.remove(selected);
            totalWeight -= selected.weight;
        }

        return result;
    }

    private record WeightedCourse(Course course, double weight) {}

    public List<Course> findByIds(List<Long> courseIds) {
        return courseRepository.findAllById(courseIds);
    }

    public List<Course> getAllVisibleCourses() {
        return courseRepository.findByIsHiddenFalse();
    }

    @Transactional
    public Course saveCourse(Course course) {
        // 중복 체크 (이름 + 주소)
        List<Course> existing = courseRepository.findByNameContainingAndIsHiddenFalse(course.getName());
        for (Course existingCourse : existing) {
            if (existingCourse.getAddress() != null &&
                existingCourse.getAddress().equals(course.getAddress())) {
                return existingCourse;  // 이미 존재하면 기존 장소 반환
            }
        }

        // 새 장소 저장
        return courseRepository.save(course);
    }

    @Transactional
    public void updatePlaceImage(Long courseId, String imageUrl) {
        courseRepository.findById(courseId).ifPresent(course -> {
            course.updatePlaceImage(imageUrl);
        });
    }
}
