package hairwhere.hairwhere.controller;

import hairwhere.hairwhere.domain.Comment;
import hairwhere.hairwhere.dto.CommentRequest;
import hairwhere.hairwhere.dto.CommentResponse;
import hairwhere.hairwhere.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("comment")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성"
        , description = "댓글을 작성합니다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{photoId}")
    @Transactional
    public ResponseEntity<CommentResponse> createComment(@RequestHeader("Authorization") String token, @PathVariable Long photoId, @RequestBody CommentRequest commentRequest) {
        String content = commentRequest.getContent();
        Long parentId = commentRequest.getParentId();

        Comment savedComment = commentService.createComment(token,photoId, content, parentId);

        return ResponseEntity.ok(CommentResponse.from(savedComment));
    }

    @Operation(summary = "댓글 조회"
        , description = "photoId에 해당하는 댓글을 조회한다. parentId는 필수가 아님"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("getComments/{photoId}")
    @Transactional
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long photoId, @RequestParam(required = false) Long parentId) {
        List<Comment> comments = commentService.getPhotoComments(photoId, parentId);

        List<CommentResponse> responses = comments.stream()
            .map(CommentResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "댓글 삭제"
        , description = "댓글을 삭제합니다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("deleteComment/{commentId}")
    @Transactional
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok("댓글 삭제 성공");
    }
}
