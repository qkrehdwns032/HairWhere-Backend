package hairwhere.hairwhere.service;

import hairwhere.hairwhere.domain.User;
import hairwhere.hairwhere.dto.KakaoUserRequest;
import hairwhere.hairwhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(KakaoService.class);

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    @Value("${kakao.client.secret}")
    private String clientSecret;

    public String getKakaoAccessToken(String code) { //code로 token을 반환
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("client_secret", clientSecret);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
            new HttpEntity<>(params, headers);

        ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
            tokenUrl,
            HttpMethod.POST,
            kakaoTokenRequest,
            KakaoTokenResponse.class
        );

        return response.getBody().getAccess_token();
    }

    /**
     * 카카오 액세스 토큰으로 사용자 정보를 요청합니다.
     */
    public KakaoUserRequest getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        // "Bearer " 접두사가 있는 경우 제거
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUserRequest> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.POST,
                kakaoUserInfoRequest,
                KakaoUserRequest.class
            );
            return response.getBody();
        } catch (Exception e) {
            // 에러 로깅
            log.error("카카오 API 호출 오류: {}", e.getMessage());
            throw e; // 재발생 또는 적절한 예외 처리
        }
    }

    /**
     * 카카오 로그인 프로세스를 처리합니다.
     * 1. 인증 코드로 액세스 토큰을 요청
     * 2. 액세스 토큰으로 사용자 정보를 요청
     * 3. 사용자 정보로 DB에서 사용자를 찾거나 새로 생성
     * 4. JWT 토큰 생성
     */
    public String processKakaoLogin(String code) { // 위 두개의 메서드를 통해 프로세스 진행
        // 카카오 액세스 토큰 가져오기
        String accessToken = getKakaoAccessToken(code);

        // 액세스 토큰으로 사용자 정보 가져오기
        //KakaoUserRequest KakaoUserRequest = getKakaoUserInfo(accessToken);

        // 사용자 정보로 우리 서비스 유저 찾기 또는 생성
        //User user = userRepository.findByKakaoId(KakaoUserRequest.getKakaoId())
            //.orElseGet(() -> createKakaoUser(KakaoUserRequest));

        // JWT 토큰 생성
        //return jwtService.generateToken(user);
        return accessToken;
    }

    /**
     * 카카오 사용자 정보로 새 사용자를 생성합니다.
     */
    private User createKakaoUser(KakaoUserRequest KakaoUserRequest) {
        User user = User.builder()
            .kakaoId(KakaoUserRequest.getId())
            .nickName(KakaoUserRequest.getProperties().getNickname())
            .profileImageUrl(KakaoUserRequest.getProperties().getProfile_image())
            .build();

        return userRepository.save(user);
    }

    /**
     * 카카오 토큰 응답을 위한 내부 클래스
     */
    public static class KakaoTokenResponse {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private String expires_in;
        private String scope;
        private String refresh_token_expires_in;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        // 다른 getter/setter는 생략
    }

    //KakaoUserRequest를 User로 변경해주는 메서드 구현
    public User kakaoUserRequestToUser(KakaoUserRequest kakaoUserRequest) {
        // 먼저 해당 kakaoId로 사용자가 이미 존재하는지 확인
        User existingUser = userRepository.findByKakaoId(kakaoUserRequest.getId())
            .orElse(null);

        if (existingUser != null) {
            // 이미 존재하는 사용자라면 그대로 반환
            return existingUser;
        }

        // 존재하지 않는다면 새로 생성하고 저장
        User newUser = User.builder()
            .kakaoId(kakaoUserRequest.getId())
            .nickName(kakaoUserRequest.getProperties().getNickname())
            .profileImageUrl(kakaoUserRequest.getProperties().getProfile_image())
            .build();

        // 데이터베이스에 저장
        return userRepository.save(newUser);
    }

}
