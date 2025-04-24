package hairwhere.hairwhere.repository;

import hairwhere.hairwhere.domain.Like;
import hairwhere.hairwhere.domain.Photo;
import hairwhere.hairwhere.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPhoto(User user, Photo photo);

    List<Like> findByUser(User user);

    List<Like> findByPhoto(Photo photo);
}
