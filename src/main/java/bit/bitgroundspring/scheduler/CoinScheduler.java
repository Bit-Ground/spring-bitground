package bit.bitgroundspring.scheduler;

import bit.bitgroundspring.service.GeminiService;
import bit.bitgroundspring.service.CoinService; // CoinService를 통해 코인 심볼을 가져오기 위해 임포트
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CoinScheduler {

    private final GeminiService geminiService;
    private final CoinService coinService; // CoinService 주입

    // AI 분석을 수행할 주요 코인 심볼 목록
    // 필요에 따라 이 목록을 DB에서 가져오거나, 설정 파일에서 관리할 수도 있습니다.
    // 여기서는 예시로 상위 거래량 코인들을 기반으로 하드코딩된 목록을 사용합니다.
    // 실제 운영 환경에서는 Upbit API를 통해 실시간 인기 코인 목록을 가져와 사용하는 것이 좋습니다.
    private static final List<String> TARGET_COIN_SYMBOLS = Arrays.asList(
            "KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-SOL", "KRW-DOGE",
            "KRW-ADA", "KRW-DOT", "KRW-AVAX", "KRW-MATIC", "KRW-TRX"
            // 추가하고 싶은 다른 코인 심볼들을 여기에 나열합니다.
    );

    public CoinScheduler(GeminiService geminiService, CoinService coinService) {
        this.geminiService = geminiService;
        this.coinService = coinService;
    }

    /**
     * 매일 자정(00시 00분 00초)에 전체 시장 및 주요 코인들에 대한 AI 분석을 수행합니다.
     * `cron = "0 0 0 * * ?"`: 초 분 시 일 월 요일
     * - 초: 0
     * - 분: 0
     * - 시: 0 (자정)
     * - 일: * (매일)
     * - 월: * (매월)
     * - 요일: ? (특정 요일 지정 안 함)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleAiAnalysisBatch() {
        System.out.println("AI analysis batch scheduled task initiated at " + LocalDateTime.now());
        try {
            // CoinService에서 현재 업비트 상장된 모든 코인 심볼을 가져와 사용하는 것도 좋은 방법입니다.
            // List<String> allKrwCoinSymbols = coinService.getAllKrwCoinSymbols(); // CoinService에 이런 메서드가 있다면
            // geminiService.generateAndSaveBatchAnalysis(allKrwCoinSymbols);

            // 현재는 미리 정의된 목록을 사용합니다.
            geminiService.generateAndSaveBatchAnalysis(TARGET_COIN_SYMBOLS);
            System.out.println("AI analysis batch scheduled task completed successfully.");
        } catch (Exception e) {
            System.err.println("Error during AI analysis batch scheduled task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 개발/테스트용으로 스케줄러가 잘 동작하는지 확인하고 싶다면 아래 주석을 풀어서 사용하세요.
    // 애플리케이션 시작 후 30초 뒤에 한 번 실행됩니다. (운영 배포 시에는 주석 처리 또는 삭제)
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE) // 앱 시작 후 30초 뒤 1회 실행
    public void runAiAnalysisOnceOnStartupForTest() {
        System.out.println("TEST: AI analysis batch on startup initiated at " + LocalDateTime.now());
        try {
            geminiService.generateAndSaveBatchAnalysis(TARGET_COIN_SYMBOLS);
            System.out.println("TEST: AI analysis batch on startup completed.");
        } catch (Exception e) {
            System.err.println("TEST: Error during AI analysis batch on startup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}