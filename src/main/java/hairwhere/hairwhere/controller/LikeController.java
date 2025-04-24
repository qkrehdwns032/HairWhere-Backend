package hairwhere.hairwhere.controller;

import hairwhere.hairwhere.service.KakaoService;
import hairwhere.hairwhere.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final KakaoService kakaoService;

    @Operation(summary = "좋아요 추가/삭제"
        , description = "좋아요 추가 또는 삭제를 수행합니다. "
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{id}")
    @Transactional
    public ResponseEntity<String> addLike(@PathVariable("id") Long id, @RequestHeader("Authorization") String token) {
        Long kakaoId = kakaoService.getKakaoUserInfo(token).getId();

        boolean isLiked = likeService.isLiked(id,kakaoId);

        if(isLiked){
            return ResponseEntity.status(HttpStatus.OK).body("좋아요 삭제");
        }
        else{
            //likeService.saveUser(userId,id);
            return ResponseEntity.status(HttpStatus.OK).body("좋아요 추가");
        }

    }
}
