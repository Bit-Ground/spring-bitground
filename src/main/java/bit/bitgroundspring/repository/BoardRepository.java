package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.BoardDto;
import org.springframework.data.jpa.repository.JpaRepository;
import bit.bitgroundspring.entity.Post;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<Post, Integer> {

    @Query(value = "SELECT " +
            "p.id, u.id AS userId, u.name, p.title, p.content, p.tier, " +
            "p.likes, p.dislikes, p.is_deleted, p.created_at, p.updated_at, p.category " +
            "FROM posts p " +
            "JOIN users u ON p.user_id = u.id " +
            "ORDER BY p.id DESC", nativeQuery = true)
    List<BoardDto> findAllBoardDtos();

}