package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.repository.CoinRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // @Transactional 임포트 유지
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional // 트랜잭션 관리 활성화
public class CoinService {

    private final CoinRepository coinRepository;
    private final WebClient upbitWebClient;
    private final ObjectMapper objectMapper;

    public CoinService(CoinRepository coinRepository, WebClient.Builder webClientBuilder) {
        this.coinRepository = coinRepository;
        this.upbitWebClient = webClientBuilder.baseUrl("https://api.upbit.com").build();
        this.objectMapper = new ObjectMapper();
    }
    
}