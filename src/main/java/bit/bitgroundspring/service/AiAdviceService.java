// src/main/java/bit/bitgroundspring/service/AiAdviceService.java

package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.AiAdviceDto;
import bit.bitgroundspring.dto.InvestmentSummaryDto;
import bit.bitgroundspring.entity.AiAdvice;
import bit.bitgroundspring.entity.Season;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.AiAdviceRepository;
import bit.bitgroundspring.repository.SeasonRepository;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAdviceService {

    private final InvestmentAnalysisService investmentAnalysisService;
    private final GeminiService geminiService;
    private final AiAdviceRepository aiAdviceRepository;
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;

    /**
     * 특정 사용자 및 시즌에 대한 AI 투자 조언을 생성하고 저장합니다.
     * 이미 조언이 존재하는 경우, 새로운 조언을 생성하지 않고 기존 조언을 반환합니다.
     *
     * @param userId 조언을 생성할 사용자의 ID
     * @param seasonId 조언을 생성할 시즌의 ID
     * @return 생성되거나 조회된 AiAdviceDto
     */
    @Transactional // 데이터베이스 쓰기 작업이 포함되므로 트랜잭션 처리
    public AiAdviceDto generateOrGetAiAdvice(Integer userId, Integer seasonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Season not found with ID: " + seasonId));

        // 1. 이미 해당 사용자 및 시즌에 대한 AI 조언이 있는지 확인
        Optional<AiAdvice> existingAdvice = aiAdviceRepository.findByUserAndSeason(user, season);
        if (existingAdvice.isPresent()) {
            log.info("AI Advice already exists for user {} in season {}. Returning existing advice (ID: {}).",
                    userId, seasonId, existingAdvice.get().getId());
            return convertToDto(existingAdvice.get());
        }

        log.info("Generating new AI Advice for user {} in season {}.", userId, seasonId);

        // 2. InvestmentAnalysisService를 통해 투자 요약 데이터 생성
        InvestmentSummaryDto summaryDto = investmentAnalysisService.getUserInvestmentSummaryForSeason(userId, seasonId);
        if (summaryDto == null) {
            log.error("Failed to generate investment summary for user {} in season {}.", userId, seasonId);
            return null; // 또는 적절한 예외 처리
        }

        // 3. GeminiService를 통해 AI 조언 생성
        AiAdviceDto generatedAdviceDto = geminiService.generateInvestmentAdvice(summaryDto);
        if (generatedAdviceDto == null) {
            log.error("Failed to generate AI advice from Gemini for user {} in season {}.", userId, seasonId);
            return null; // 또는 적절한 예외 처리
        }

        // 4. 생성된 조언을 AiAdvice 엔티티로 변환하여 저장
        AiAdvice newAdvice = AiAdvice.builder()
                .user(user)
                .season(season)
                .score(generatedAdviceDto.getScore())
                .advice(generatedAdviceDto.getAdvice())
                .createdAt(LocalDateTime.now()) // 현재 시간으로 설정
                .build();

        AiAdvice savedAdvice = aiAdviceRepository.save(newAdvice);
        log.info("AI Advice saved successfully for user {} in season {}. Advice ID: {}",
                userId, seasonId, savedAdvice.getId());

        // 저장된 엔티티 정보를 포함한 DTO 반환
        return convertToDto(savedAdvice);
    }

    /**
     * 특정 사용자 및 시즌에 대한 AI 투자 조언을 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param seasonId 조회할 시즌의 ID
     * @return 조회된 AiAdviceDto (존재할 경우) 또는 null
     */
    public AiAdviceDto getAiAdvice(Integer userId, Integer seasonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Season not found with ID: " + seasonId));

        Optional<AiAdvice> adviceOptional = aiAdviceRepository.findByUserAndSeason(user, season);
        return adviceOptional.map(this::convertToDto).orElse(null);
    }

    /**
     * AiAdvice 엔티티를 AiAdviceDto로 변환합니다.
     *
     * @param aiAdvice 변환할 AiAdvice 엔티티
     * @return 변환된 AiAdviceDto
     */
    private AiAdviceDto convertToDto(AiAdvice aiAdvice) {
        return AiAdviceDto.builder()
                .id(aiAdvice.getId())
                .userId(aiAdvice.getUser().getId())
                .seasonId(aiAdvice.getSeason().getId())
                .score(aiAdvice.getScore())
                .advice(aiAdvice.getAdvice())
                .createdAt(aiAdvice.getCreatedAt())
                .build();
    }
}
