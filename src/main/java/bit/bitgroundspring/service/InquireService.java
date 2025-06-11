package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.InquireRequestDto;
import bit.bitgroundspring.entity.Inquiry;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.InquireRepository;
import bit.bitgroundspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InquireService {
    private final InquireRepository inquireRepository;
    private final UserRepository userRepository;

    public void createInquiry(InquireRequestDto dto) {
        User user = userRepository.findById(dto.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException());

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(dto.getTitle())
                .content(dto.getContent())
                .isAnswered(false)
                .build();
        inquireRepository.save(inquiry);

    }
}
