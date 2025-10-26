package com.CUK.geulDa.global.config;

import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import com.CUK.geulDa.domain.postcard.PostCard;
import com.CUK.geulDa.domain.postcard.repository.PostCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PlaceRepository placeRepository;
    private final PostCardRepository postCardRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 데이터가 있으면 초기화하지 않음
        if (placeRepository.count() > 0) {
            log.info("초기 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("초기 데이터 생성 시작...");
        initializePlacesAndPostcards();
        log.info("초기 데이터 생성 완료!");
    }

    private void initializePlacesAndPostcards() {
        // 1. 김수환관
        Place place1 = createPlace(
                "김수환관",
                "가톨릭대학교 부천캠퍼스의 상징적인 건물로, 학생들의 학습과 교류 공간입니다.",
                "경기도 부천시 지봉로 43",
                37.5034,
                126.7633,
                "https://example.com/video/kimsoohwan.mp4",
                "https://example.com/image/kimsoohwan.jpg",
                "김수환관 스탬프를 획득했습니다! 이곳에서 많은 학생들이 꿈을 키워갑니다."
        );
        createPostcards(place1, "김수환관에서의 특별한 순간", "숨겨진 김수환관의 이야기");

        // 2. 다솔관
        Place place2 = createPlace(
                "다솔관",
                "학생들의 창의적인 활동과 휴식을 위한 복합 문화 공간입니다.",
                "경기도 부천시 지봉로 43",
                37.5038,
                126.7638,
                "https://example.com/video/dasol.mp4",
                "https://example.com/image/dasol.jpg",
                "다솔관 스탬프를 획득했습니다! 학생들의 꿈이 모이는 곳입니다."
        );
        createPostcards(place2, "다솔관에서 만난 친구들", "다솔관에 숨겨진 비밀");

        // 3. 중도
        Place place3 = createPlace(
                "중도",
                "가톨릭대학교의 중앙도서관으로, 지식의 보물창고입니다.",
                "경기도 부천시 지봉로 43",
                37.5041,
                126.7641,
                "https://example.com/video/library.mp4",
                "https://example.com/image/library.jpg",
                "중도 스탬프를 획득했습니다! 지식의 바다에서 영감을 얻으세요."
        );
        createPostcards(place3, "중도에서의 조용한 공부 시간", "밤의 중도, 특별한 추억");

        // 4. 한국 만화 박물관
        Place place4 = createPlace(
                "한국 만화 박물관",
                "한국 만화의 역사와 문화를 한눈에 볼 수 있는 특별한 공간입니다.",
                "경기도 부천시 원미구 길주로 1",
                37.4847,
                126.7828,
                "https://example.com/video/cartoon.mp4",
                "https://example.com/image/cartoon.jpg",
                "한국 만화 박물관 스탬프를 획득했습니다! 만화 속 주인공이 되어보세요."
        );
        createPostcards(place4, "만화 속 세상으로의 여행", "숨겨진 명작 만화의 이야기");

        // 5. 원미산 진달래 동산
        Place place5 = createPlace(
                "원미산 진달래 동산",
                "봄이면 진달래가 만발하는 아름다운 산책로입니다.",
                "경기도 부천시 원미구 춘의동 산 5",
                37.5089,
                126.7756,
                "https://example.com/video/azalea.mp4",
                "https://example.com/image/azalea.jpg",
                "원미산 진달래 동산 스탬프를 획득했습니다! 봄의 향기를 느껴보세요."
        );
        createPostcards(place5, "진달래꽃이 피는 계절", "원미산의 숨겨진 전망대");

        // 6. 상동호수공원
        Place place6 = createPlace(
                "상동호수공원",
                "도심 속 자연을 즐길 수 있는 평화로운 호수 공원입니다.",
                "경기도 부천시 원미구 상동 546-1",
                37.5012,
                126.7589,
                "https://example.com/video/lake.mp4",
                "https://example.com/image/lake.jpg",
                "상동호수공원 스탬프를 획득했습니다! 호수의 잔잔한 물결처럼 평온한 하루 되세요."
        );
        createPostcards(place6, "호수에 비친 하늘", "호수공원의 밤, 별빛 아래에서");

        // 7. 부천자유시장
        Place place7 = createPlace(
                "부천자유시장",
                "부천의 전통과 활기가 살아있는 재래시장입니다.",
                "경기도 부천시 원미구 송내대로 19번길 14",
                37.4889,
                126.7523,
                "https://example.com/video/market.mp4",
                "https://example.com/image/market.jpg",
                "부천자유시장 스탬프를 획득했습니다! 인심 좋은 상인들의 미소를 만나보세요."
        );
        createPostcards(place7, "시장의 맛과 정", "시장 골목의 숨은 맛집");

        // 8. 부천역 마루광장
        Place place8 = createPlace(
                "부천역 마루광장",
                "부천의 중심에서 만나는 열린 광장, 문화와 사람이 모이는 곳입니다.",
                "경기도 부천시 원미구 부천로 1",
                37.4837,
                126.7829,
                "https://example.com/video/maru.mp4",
                "https://example.com/image/maru.jpg",
                "부천역 마루광장 스탬프를 획득했습니다! 부천의 활기를 느껴보세요."
        );
        createPostcards(place8, "광장에서의 특별한 만남", "마루광장의 숨겨진 포토존");

        // 9. 부천호수식물원 수피아
        Place place9 = createPlace(
                "부천호수식물원 수피아",
                "다양한 식물과 함께하는 힐링 공간, 자연의 아름다움을 느껴보세요.",
                "경기도 부천시 원미구 소사로 110번길 95",
                37.4756,
                126.7934,
                "https://example.com/video/sopia.mp4",
                "https://example.com/image/sopia.jpg",
                "수피아 스탬프를 획득했습니다! 식물들이 전하는 자연의 메시지를 들어보세요."
        );
        createPostcards(place9, "식물원에서의 힐링 타임", "수피아의 비밀 정원");

        // 10. 부천아트벙커
        Place place10 = createPlace(
                "부천아트벙커",
                "옛 방공호를 개조한 독특한 문화예술 공간입니다.",
                "경기도 부천시 원미구 길주로 1",
                37.4841,
                126.7835,
                "https://example.com/video/artbunker.mp4",
                "https://example.com/image/artbunker.jpg",
                "부천아트벙커 스탭프를 획득했습니다! 예술의 지하 세계로 초대합니다."
        );
        createPostcards(place10, "지하 예술의 향연", "아트벙커의 숨겨진 작품");

        log.info("10개 명소와 20개 엽서(일반 10개, 히든 10개) 생성 완료");
    }

    private Place createPlace(String name, String description, String address,
                              Double latitude, Double longitude,
                              String video, String placeImg, String systemMessage) {
        Place place = Place.builder()
                .name(name)
                .description(description)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .isHidden(false)
                .video(video)
                .placeImg(placeImg)
                .systemMessage(systemMessage)
                .build();

        return placeRepository.save(place);
    }

    private void createPostcards(Place place, String normalMessage, String hiddenMessage) {
        // 일반 엽서
        PostCard normalPostcard = PostCard.builder()
                .place(place)
                .message(normalMessage)
                .imageUrl("https://example.com/postcard/normal/" + place.getId() + ".jpg")
                .isHidden(false)
                .build();
        postCardRepository.save(normalPostcard);

        // 히든 엽서
        PostCard hiddenPostcard = PostCard.builder()
                .place(place)
                .message(hiddenMessage)
                .imageUrl("https://example.com/postcard/hidden/" + place.getId() + ".jpg")
                .isHidden(true)
                .build();
        postCardRepository.save(hiddenPostcard);
    }
}
