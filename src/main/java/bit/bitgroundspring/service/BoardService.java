package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.BoardDto;
import bit.bitgroundspring.entity.Post;
import bit.bitgroundspring.entity.User;
import bit.bitgroundspring.repository.UserRepository;
import org.springframework.stereotype.Service;

import bit.bitgroundspring.repository.BoardRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    
}
