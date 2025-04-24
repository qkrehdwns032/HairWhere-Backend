package hairwhere.hairwhere.repository;

import hairwhere.hairwhere.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 최상위 댓글 조회 (parentId가 null인 경우)
    List<Comment> findByPhotoIdAndParentIdIsNullOrderByCreatedAtDesc(Long photoId);

    // 특정 댓글의 답글 조회
    List<Comment> findByPhotoIdAndParentIdOrderByCreatedAtDesc(Long photoId, Long parentId);
}
