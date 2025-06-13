package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.AnswerDto;
import bit.bitgroundspring.dto.InquireRequestDto;
import bit.bitgroundspring.dto.InquireResponseDto;
import bit.bitgroundspring.entity.Inquiry;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.InquireRepository;
import bit.bitgroundspring.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    public void deleteInquiry(Integer id) {
        if (!inquireRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 문의사항이 존재하지 않습니다.");
        }

        inquireRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<InquireResponseDto> getPagedInquiries(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return inquireRepository.findByIsDeletedFalse(pageRequest)
                .map(InquireResponseDto::new);
    }

    // InquireService.java
    @Transactional
    public void updateAnswer(Integer inquiryId, AnswerDto dto, String adminUsername) {
        Inquiry inquiry = inquireRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("Inquiry not found"));

        inquiry.setAnswer(dto.getContent());
        inquiry.setAnswerWriter(adminUsername);
        inquiry.setAnsweredAt(LocalDateTime.now());
        inquiry.setIsAnswered(true);
    }
}
