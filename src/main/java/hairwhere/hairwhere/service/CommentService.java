package hairwhere.hairwhere.service;

import hairwhere.hairwhere.domain.Comment;
import hairwhere.hairwhere.domain.Photo;
import hairwhere.hairwhere.domain.User;
import hairwhere.hairwhere.repository.CommentRepository;
import hairwhere.hairwhere.repository.PhotoRepository;
import hairwhere.hairwhere.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final KakaoService kakaoService;

    public Comment createComment(@RequestHeader("Authorization") String token, Long PhotoId, String content, Long parentId) {
        Photo photo = photoRepository.findById(PhotoId)
            .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        // 현재 로그인한 사용자 정보 가져오기
        User user = kakaoService.kakaoUserRequestToUser(kakaoService.getKakaoUserInfo(token));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPhoto(photo);
        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            comment.setParent(parentComment);
        }

        return commentRepository.save(comment);
    }

    public List<Comment> getPhotoComments(Long PhotoId, Long parentId) {
        if(parentId == null){
            return commentRepository.findByPhotoIdAndParentIdIsNullOrderByCreatedAtDesc(PhotoId);
        }

        return commentRepository.findByPhotoIdAndParentIdOrderByCreatedAtDesc(PhotoId, parentId);
    }

    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        commentRepository.delete(comment);
    }
}
