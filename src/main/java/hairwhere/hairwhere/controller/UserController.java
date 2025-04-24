package hairwhere.hairwhere.controller;

import hairwhere.hairwhere.domain.Like;
import hairwhere.hairwhere.domain.User;
import hairwhere.hairwhere.dto.KakaoUserRequest;
import hairwhere.hairwhere.dto.PhotoResponse;
import hairwhere.hairwhere.repository.UserRepository;
import hairwhere.hairwhere.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.util.List;

//logger 추가
import org.slf4j.Logger;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kakao")
public class UserController {

    private final KakaoService kakaoService;
    private final LikeService likeService;
    private final PhotoService photoService;
    private final UserRepository userRepository;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);

    @Value("${kakao.client.id}")
    private String clientId;

    @Operation(summary = "카카오 로그인"
        , description = "카카오 로그인 URL을 반환합니다. "
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/getCode")
    public ResponseEntity<Void> kakaoLogin() {
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize" +
            "?client_id=" + clientId +
            "&redirect_uri=" + "http://localhost:8080/kakao/auth/kakao/callback" +
            "&response_type=code";

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(kakaoAuthUrl));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Operation(summary = "Redirect URI"
        , description = "카카오 로그인 후 Redirect URI입니다. "
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/oauth")
    public ResponseEntity<String> kakaoCallback(@RequestParam String code) {

        return ResponseEntity.ok(code);
    }

    // 토큰 응답 DTO
    public static class TokenResponse {
        private String accessToken;

        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    @Operation(summary = "code르 access token 요청"
        , description = "code로 accesstoken을 받습니다. "
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/getAccessToken") // 카카오에서 받은 code로 access token을 요청
    public ResponseEntity<String> getAccessToken(@RequestParam String code) {
        String accessToken = kakaoService.getKakaoAccessToken(code);

        log.info("Access Token: {}", accessToken);

        return ResponseEntity.ok(accessToken);
    }

    @Operation(summary = "accesstoken을 이용해 유저 정보를 받습니다"
        , description = "accesstoken을 이용해 유저 정보를 받습니다"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/getUserInfo") // access token으로 카카오 유저 정보를 요청
    public ResponseEntity<KakaoUserRequest> getUserInfo(@RequestHeader("Authorization") String token) {
        KakaoUserRequest userInfo = kakaoService.getKakaoUserInfo(token);
        return ResponseEntity.ok(userInfo);
    }

    @Operation(summary = "logout 서버에서 특별한 처리 없음"
        , description = "logout 서버에서 특별한 처리 없음"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // 서버 측에서는 특별한 처리 없음
        return ResponseEntity.ok("로그아웃 성공");
    }

    @Operation(summary = "token을 사용해 본인의 user 정보를 받습니다."
        , description = "token을 사용해 본인의 user 정보를 받습니다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/mypage")
    public ResponseEntity<User> getMyPage(@RequestHeader("Authorization") String token) {
        User user = kakaoService.kakaoUserRequestToUser(kakaoService.getKakaoUserInfo(token));

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "token을 사용해 본인이 좋아요한 페이지들을 받습니다."
        , description = "token을 사용해 본인이 좋아요한 페이지들을 받습니다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/mypage/like")
    public Page<PhotoResponse> getMyLikePage(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "15") int size,
                                             @RequestParam(defaultValue = "created") String sortBy,
                                             @RequestParam(defaultValue = "desc") String sortOrder,
                                             @RequestHeader("Authorization") String token) {

        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        User user = kakaoService.kakaoUserRequestToUser(kakaoService.getKakaoUserInfo(token));

        List<Like> likes = likeService.findByUser(user);//user를 기준으로 좋아요한 게시글들 찾기

        return photoService.findLikedPhotosByUser(likes, pageable);
    }

    @Operation(summary = "kakaoId를 받아 user의 페이지들을 받습니다."
        , description = "kakaoId를 받아 user의 페이지들을 받습니다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("find/{kakaoId}/photos")
    public Page<PhotoResponse> findUserPhotos(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "15") int size,
                                              @RequestParam(defaultValue = "created") String sortBy,
                                              @RequestParam(defaultValue = "desc") String sortOrder,
                                              @PathVariable Long kakaoId) {
        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        return photoService.findByKakaoId(kakaoId, pageable);
    }

    @Operation(summary = "kakaoId를 받아 해당 유저의 정보를 받습니다."
        , description = "kakaoId를 받아 해당 유저의 정보를 받습니다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("find/{kakaoId}")
    public ResponseEntity<User> findByName(@PathVariable Long kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
            .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
