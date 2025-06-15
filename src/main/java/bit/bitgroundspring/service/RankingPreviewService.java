package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.PublicRankingDto;
import bit.bitgroundspring.dto.projection.RankProjection;
import bit.bitgroundspring.dto.response.PublicRankingResponse;
import bit.bitgroundspring.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingPreviewService {

    private final RankRepository rankRepository;

    public PublicRankingResponse getPublicRankingPreview() {
        List<RankProjection> projections = rankRepository.findCurrentSeasonRankings();

        if (projections.isEmpty()) {
            return PublicRankingResponse.builder()
                    .seasonName("시즌 정보 없음")
                    .updatedAtText("-")
                    .minutesLeftText("-")
                    .rankings(List.of())
                    .build();
        }

        RankProjection first = projections.get(0);
        LocalDateTime updatedAt = first.getUpdatedAt();
        String seasonName = "데브 시즌 " + first.getSeasonId();
        String updatedAtText = formatUpdatedTime(updatedAt);
        String minutesLeftText = formatMinutesLeft(updatedAt);

        List<PublicRankingDto> rankingDtos = projections.stream()
                .limit(5)
                .map(p -> PublicRankingDto.builder()
                        .name(p.getName())
                        .profileImage(p.getProfileImage())
                        .tier(p.getTier())
                        .totalValue(p.getTotalValue())
                        .ranks(p.getRanks())
                        .build()
                ).collect(Collectors.toList());

        return PublicRankingResponse.builder()
                .seasonName(seasonName)
                .updatedAtText(updatedAtText)
                .minutesLeftText(minutesLeftText)
                .rankings(rankingDtos)
                .build();
    }

    private String formatUpdatedTime(LocalDateTime time) {
        String meridiem = time.getHour() < 12 ? "오전" : "오후";
        int hour12 = time.getHour() % 12 == 0 ? 12 : time.getHour() % 12;
        return String.format("%d월 %d일 %s %d시 기준", time.getMonthValue(), time.getDayOfMonth(), meridiem, hour12);
    }

    private String formatMinutesLeft(LocalDateTime updatedAt) {
        LocalDateTime nextHour = updatedAt.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        long minutesLeft = java.time.Duration.between(LocalDateTime.now(), nextHour).toMinutes();
        minutesLeft = Math.max(0, minutesLeft); // 음수 방지
        return String.format("다음 갱신까지 %d분 남았습니다", minutesLeft);
    }
}