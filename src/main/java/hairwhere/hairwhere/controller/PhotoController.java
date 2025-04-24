package hairwhere.hairwhere.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hairwhere.hairwhere.domain.Photo;
import hairwhere.hairwhere.domain.User;
import hairwhere.hairwhere.dto.PhotoResponse;
import hairwhere.hairwhere.dto.SearchRequest;
import hairwhere.hairwhere.dto.UploadRequest;
import hairwhere.hairwhere.dto.UploadResponse;
import hairwhere.hairwhere.repository.PhotoRepository;
import hairwhere.hairwhere.repository.UserRepository;
import hairwhere.hairwhere.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("photo")
public class PhotoController {

    private final PhotoService photoService;
    private final LikeService likeService;
    private final PhotoRepository photoRepository;

    private static final Logger logger = LoggerFactory.getLogger(PhotoController.class);
    private final UserRepository userRepository;
    private final KakaoService kakaoService;
    private final JwtService jwtService;

    @Operation(summary = "token을 사용해서 사진 업로드"
        , description = "token을 사용해서 사진 업로드"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@ModelAttribute("uploadRequest") UploadRequest uploadRequest,
                                                 @RequestHeader("Authorization") String token) {
        try {
            // Bearer 토큰에서 실제 토큰 추출
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UploadResponse("유효하지 않은 토큰 형식입니다."));
            }

            // "Bearer " 접두사 제거
            String actualToken = token.substring(7);

            // 토큰에서 카카오 ID 추출
            //Long kakaoId = jwtService.extractKakaoId(actualToken);

            // 받은 DTO 데이터 로깅
            logger.info("받은 UploadRequest: {}", uploadRequest);
            if (uploadRequest.getImage() != null) {
                logger.info("받은 파일 수: {}", uploadRequest.getImage().length);
                for (MultipartFile file : uploadRequest.getImage()) {
                    logger.info("파일 이름: {}, 크기: {}",
                        file.getOriginalFilename(),
                        file.getSize());
                }
            } else {
                logger.info("받은 파일 없음");
            }

            // 카카오 ID로 사용자 조회
            User user = kakaoService.kakaoUserRequestToUser(kakaoService.getKakaoUserInfo(token));

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UploadResponse("사용자를 찾을 수 없습니다."));
            }

            logger.info("토큰에서 추출한 사용자 '{}'", user.getNickName());

            // 이미지 null 체크
            MultipartFile[] image = uploadRequest.getImage();
            if (image == null || image.length == 0) {
                return ResponseEntity.badRequest()
                    .body(new UploadResponse("이미지 파일이 없습니다."));
            }

            if (image.length > 3) {
                return ResponseEntity.badRequest()
                    .body(new UploadResponse("이미지는 최대 3개까지만 업로드 가능합니다."));
            }

            // 이미지 타입 체크
            for (MultipartFile file : image) {
                String contentType = file.getContentType();
                logger.info("파일: {}, 콘텐츠 타입: {}", file.getOriginalFilename(), contentType);

                if (!contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest()
                        .body(new UploadResponse("잘못된 파일 유형입니다."));
                }
            }

            // 날짜 처리
            String createdStr = uploadRequest.getCreatedStr();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime created = LocalDateTime.parse(createdStr, formatter);

            int likeCount = 0;
            Long id = photoService.upload(user.getNickName(), image, likeCount, created, user, uploadRequest);

            logger.info("photoService.upload() 성공적으로 완료");

            return ResponseEntity.ok(new UploadResponse(user, id));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                .body(new UploadResponse("날짜 형식이 올바르지 않습니다."));
        } catch (Exception e) {
            logger.error("업로드 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UploadResponse("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(summary = "메인페이지, 전체 사진을 불러온다."
        , description = "메인페이지, 전체 사진을 불러온다"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/find/all")
    public Page<PhotoResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "15") int size,
                                       @RequestParam(defaultValue = "created") String sortBy,
                                       @RequestParam(defaultValue = "desc") String sortOrder) {

        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        return photoService.findAll(pageable);
    }

    @Operation(summary = "hairSalon을 기준으로 사진을 불러온다."
        , description = "hairSalon을 기준으로 사진을 불러온다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/findHair/{hairSalon}")
    public Page<PhotoResponse> findByHairSalon(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "15") int size,
                                               @RequestParam(defaultValue = "created") String sortBy,
                                               @RequestParam(defaultValue = "desc") String sortOrder,
                                               @PathVariable("hairSalon") String hairSalon) {

        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        return photoService.findByHairSalon(hairSalon, pageable);
    }

    @Operation(summary = "사진을 선택핸 경우, 해당 사진에 대한 정보를 불러온다."
        , description = "사진을 선택핸 경우, 해당 사진에 대한 정보를 불러온다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/find/{id}")
    public PhotoResponse search(@PathVariable("id") Long id) {
        return photoService.findById(id);
    }

    @Operation(summary = "id에 대한 게시글에 좋아요를 한 사용자들의 목록"
        , description = "id에 대한 게시글에 좋아요를 한 사용자들의 목록"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/find/{id}/likes")// 게시글에 좋아요를 한 사용자들의 목록
    public ResponseEntity<List<User>> getUserWhoPhotoLiked(@PathVariable("id") Long id) {
        Photo photo = photoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("사진을 찾을 수 없습니다."));

        List<User> users = likeService.getUserWhoPhotoLiked(photo);

        return ResponseEntity.ok(users);
    }

    @Operation(summary = "id에 대해 해당 게시글을 삭제한다."
        , description = "id에 대해 해당 게시글을 삭제한다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id, @RequestHeader("Authorization") String token) {
        User user = kakaoService.kakaoUserRequestToUser(kakaoService.getKakaoUserInfo(token));
        String name = user.getNickName();

        photoService.deletePhoto(id, name);//게시글 id와 session에 저장된 name
        return "삭제 완료";
    }

    @Operation(summary = "gender를 기준으로 사진을 불러온다."
        , description = "gender를 기준으로 사진을 불러온다. male, female로 하면 될 듯"
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/findByGender/{gender}")
    public Page<PhotoResponse> findByGender(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "15") int size,
                                            @RequestParam(defaultValue = "created") String sortBy,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @PathVariable("gender") String gender) {

        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        return photoService.findByGender(gender, pageable);
    }

    @Operation(summary = "hairSalonAddress를 받아 사진을 불러온다."
        , description = "hairSalonAddress를 받아 사진을 불러온다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/find/address/{hairSalonAddress}")
    public Page<PhotoResponse> findByAddress(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "15") int size,
                                             @RequestParam(defaultValue = "created") String sortBy,
                                             @RequestParam(defaultValue = "desc") String sortOrder,
                                             @PathVariable("hairSalonAddress") String hairSalonAddress) {

        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        return photoService.findByHairSalonAddress(hairSalonAddress, pageable);
    }

    @Operation(summary = "SearchRequest를 받아 사진을 불러온다"
        , description = "SearchRequest를 받아 사진을 불러온다."
        , responses = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<PhotoResponse>> search(HttpServletRequest request, @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "15") int size,
                                                      @RequestParam(defaultValue = "created") String sortBy,
                                                      @RequestParam(defaultValue = "desc") String sortOrder,
                                                      @ModelAttribute SearchRequest searchRequest) throws UnsupportedEncodingException {

        HttpSession session = request.getSession();

        Sort sort = Sort.by(Sort.Order.by(sortBy).with(Sort.Direction.fromString(sortOrder)));

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok()
            .body(photoService.search(searchRequest, pageable));
    }
}
