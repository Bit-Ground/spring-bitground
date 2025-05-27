package bit.bitgroundspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import bit.bitgroundspring.entity.Post;

public interface BoardRepository extends JpaRepository<Post, Integer> {

}