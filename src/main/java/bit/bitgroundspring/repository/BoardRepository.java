package bit.bitgroundspring.repository;

import bit.bitgroundspring.dto.BoardDto;
import org.springframework.data.jpa.repository.JpaRepository;
import bit.bitgroundspring.entity.Post;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<Post, Integer> {

    @Query("SELECT new bit.bitgroundspring.dto.BoardDto(" +
            "p.id, u.id, u.name, p.title, p.content, p.filePath, p.fileName, " +
            "p.likes, p.reports, p.createdAt, p.updatedAt, p.deletedAt, p.isDeleted, p.category) " +
            "FROM Post p JOIN p.user u ORDER BY p.id DESC")
    List<BoardDto> findAllBoardDtos();
}