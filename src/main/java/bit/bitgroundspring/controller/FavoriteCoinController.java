package bit.bitgroundspring.controller;

import bit.bitgroundspring.dto.UserDto;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.security.oauth2.AuthService;
import bit.bitgroundspring.service.FavoriteCoinService;
import bit.bitgroundspring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteCoinController {

    private final FavoriteCoinService favService;

    // 토큰 → UserDto → User 변환은 이 리졸버가 담당
    @PostMapping
    public ResponseEntity<Void> addFav(
            @RequestParam Integer userId,
            @RequestParam String symbol
    ) {
        favService.addFavorite(userId, symbol);
        URI location = URI.create("/api/favorites/" + symbol);
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFav(
            @RequestParam Integer userId,
            @PathVariable String symbol
    ) {
        favService.removeFavorite(userId, symbol);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<String>> listFav(
            @RequestParam Integer userId
    ) {
        List<String> symbols = favService.listFavorites(userId);
        return ResponseEntity.ok(symbols);
    }
}