// src/main/java/bit/bitgroundspring/controller/AiAdviceController.java

package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.AiAdviceDto;
import bit.bitgroundspring.service.AiAdviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advice") // API 기본 경로 설정
@RequiredArgsConstructor
@Slf4j
public class AiAdviceController {

    private final AiAdviceService aiAdviceService;

    /**
     * 특정 사용자 및 시즌에 대한 AI 투자 조언을 생성하거나 기존 조언을 조회합니다.
     * 이 엔드포인트는 클라이언트에서 AI 조언이 필요할 때 호출됩니다.
     * 처음 호출 시 조언을 생성하고 저장하며, 이후 호출 시에는 저장된 조언을 반환합니다.
     *
     * POST 요청으로 생성 (멱등성 고려)
     * GET 요청으로 조회 (일반적인 RESTful API 관행)
     * 여기서는 요청의 목적(생성 또는 조회)을 명확히 하고, GET 요청으로 조회만 하는 것이 더 적절합니다.
     * 생성은 관리자 기능이거나, 특정 스케줄에 따라 배치로 이루어지는 것이 일반적입니다.
     * 따라서, 생성은 POST로, 조회는 GET으로 분리하거나, 아니면 GET 요청이 없으면 생성하는 로직으로 구성합니다.
     * 현재 `AiAdviceService`의 `generateOrGetAiAdvice` 메서드는 조회 후 없으면 생성하는 로직을 포함하므로,
     * 여기서는 GET 요청으로 통합 처리할 수 있습니다.
     *
     * @param userId 조언을 요청할 사용자의 ID
     * @param seasonId 조언을 요청할 시즌의 ID
     * @return AI 조언 데이터를 담은 AiAdviceDto
     */
    @GetMapping("/user/{userId}/season/{seasonId}")
    public ResponseEntity<AiAdviceDto> getAiAdviceForUserAndSeason(
            @PathVariable Integer userId,
            @PathVariable Integer seasonId) {
        try {
            // AiAdviceService의 generateOrGetAiAdvice 메서드를 호출하여 조언을 가져오거나 생성
            AiAdviceDto adviceDto = aiAdviceService.generateOrGetAiAdvice(userId, seasonId);

            if (adviceDto != null) {
                log.info("Successfully retrieved/generated AI Advice for user {} in season {}.", userId, seasonId);
                return ResponseEntity.ok(adviceDto); // 성공적으로 응답 반환 (HTTP 200 OK)
            } else {
                log.warn("AI Advice could not be generated or found for user {} in season {}.", userId, seasonId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 내부 서버 오류 응답
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for AI Advice request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 사용자 또는 시즌을 찾을 수 없는 경우
        } catch (Exception e) {
            log.error("Error while getting/generating AI Advice for user {} in season {}: {}", userId, seasonId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 기타 예외 처리
        }
    }

    // AI 조언을 명시적으로 '생성'만 하는 POST 엔드포인트 (선택적)
    // 만약 `generateOrGetAiAdvice` 대신 '무조건 생성' 또는 '재생성' 로직이 필요하다면 사용.
    // 현재는 `generateOrGetAiAdvice`가 조회 후 생성 로직을 포함하므로 필수는 아님.
    /*
    @PostMapping("/user/{userId}/season/{seasonId}/generate")
    public ResponseEntity<AiAdviceDto> generateNewAiAdviceForUserAndSeason(
            @PathVariable Integer userId,
            @PathVariable Integer seasonId) {
        try {
            // 이 메서드는 AiAdviceService에 '무조건 생성'하는 메서드가 필요합니다.
            // (예: aiAdviceService.generateAiAdviceAlways(userId, seasonId) )
            AiAdviceDto adviceDto = aiAdviceService.generateOrGetAiAdvice(userId, seasonId); // 현재는 기존 로직 재사용
            if (adviceDto != null) {
                log.info("Successfully initiated AI Advice generation for user {} in season {}.", userId, seasonId);
                return ResponseEntity.status(HttpStatus.CREATED).body(adviceDto);
            } else {
                log.warn("AI Advice generation failed for user {} in season {}.", userId, seasonId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for AI Advice generation request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error during AI Advice generation for user {} in season {}: {}", userId, seasonId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    */
}
