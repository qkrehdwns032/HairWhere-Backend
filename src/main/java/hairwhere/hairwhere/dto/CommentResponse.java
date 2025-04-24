package hairwhere.hairwhere.dto;

import hairwhere.hairwhere.domain.Comment;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private UserResponse user;  // User 엔티티 대신 UserResponse 사용
    private Long photoId;
    private Long parentId;
    private List<CommentResponse> replies = new ArrayList<>();

    @Getter
    @Setter
    public static class UserResponse {  // 내부 정적 클래스로 UserResponse 정의
        private Long id;
        private Long kakaoId;
        private String nickName;
        private String profileImageUrl;

        public static UserResponse from(hairwhere.hairwhere.domain.User user) {
            UserResponse response = new UserResponse();
            response.setId(user.getId());
            response.setKakaoId(user.getKakaoId());
            response.setNickName(user.getNickName());
            response.setProfileImageUrl(user.getProfileImageUrl());
            return response;
        }
    }

    public static CommentResponse from(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUser(UserResponse.from(comment.getUser()));  // User 정보 변환
        response.setPhotoId(comment.getPhoto().getId());

        if (comment.getParent() != null) {
            response.setParentId(comment.getParent().getId());
        }

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            response.setReplies(comment.getReplies().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList()));
        }

        return response;
    }
}
