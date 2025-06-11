package bit.bitgroundspring.repository;

import bit.bitgroundspring.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquireRepository extends JpaRepository<Inquiry, Integer> {

}
