package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.AiInsight; // AiInsight 엔티티 임포트
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiInsightRepository extends JpaRepository<AiInsight, Integer> {
    // 특정 코인 심볼과 특정 날짜에 해당하는 AI 분석 결과가 이미 있는지 확인하는 메서드
    Optional<AiInsight> findBySymbolAndDate(String symbol, LocalDate date);

    // 특정 코인 심볼에 대한 모든 AI 분석 결과를 최신순으로 가져오는 메서드
    List<AiInsight> findBySymbolOrderByCreatedAtDesc(String symbol);
}