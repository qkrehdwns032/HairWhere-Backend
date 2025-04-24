package hairwhere.hairwhere.service;

import hairwhere.hairwhere.domain.Like;
import hairwhere.hairwhere.domain.Photo;
import hairwhere.hairwhere.domain.User;
import hairwhere.hairwhere.dto.PhotoResponse;
import hairwhere.hairwhere.dto.SearchRequest;
import hairwhere.hairwhere.dto.UploadRequest;
import hairwhere.hairwhere.repository.PhotoRepository;
import hairwhere.hairwhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private final PhotoRepository photorepository;
    private final GCPStorageService gcpStorageService;

    private final int MIN_RANDOM_NUM = 1;
    //private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;

    @Transactional
    public Long upload(String nickname, MultipartFile[] image,
                       int likeCount, LocalDateTime created, User user, UploadRequest uploadRequest) {
        List<String> imagePaths = new ArrayList<>();

        for(MultipartFile file : image) {
            try {
                String imagePath = gcpStorageService.upload(file);
                imagePaths.add(imagePath);
            }
            catch (Exception e) {
                //logger.error("Error uploading file to NCP", e);
                throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
            }
        }

        String hairName = uploadRequest.getHairName();
        String text = uploadRequest.getText();
        String gender = uploadRequest.getGender();
        String hairSalon = uploadRequest.getHairSalon();
        String hairSalonAddress = uploadRequest.getHairSalonAddress();
        String hairLength = uploadRequest.getHairLength();
        String hairColor = uploadRequest.getHairColor();

        Photo photo = Photo.builder()
            .nickname(nickname)
            .photoImagePath(imagePaths)
            .likeCount(likeCount)
            .hairName(hairName)
            .text(text)
            .gender(gender)
            .created(created)
            .hairSalon(hairSalon)
            .hairSalonAddress(hairSalonAddress)
            .hairLength(hairLength)
            .hairColor(hairColor)
            .user(user)
            .build();

        photorepository.save(photo);
        return photo.getId();
    }

    @Transactional
    public Page<PhotoResponse> findByHairSalon(String hairSalon, Pageable pageable){
        Page<Photo> photo = photorepository.findByHairSalon(hairSalon,pageable);
        return photo.map(PhotoResponse::fromEntity);
    }

    @Transactional
    public Page<PhotoResponse> findByHairSalonAddress(String hairSalonAddress,Pageable pageable){
        Page<Photo> photo = photorepository.findByHairSalonAddress(hairSalonAddress,pageable);
        return photo.map(PhotoResponse::fromEntity);
    }

    @Transactional
    public PhotoResponse findById(Long id) {
        return findPhotoById(id);
    }

    @Transactional
    public Photo findById2(Long id) {
        return findPhotoById2(id);
    }

    @Transactional
    public Page<PhotoResponse> findByGender(String gender,Pageable pageable) {
        Page<Photo> photo = photorepository.findByGender(gender,pageable);
        return photo.map(PhotoResponse::fromEntity);
    }

    @Transactional
    public Page<PhotoResponse> findAll(Pageable pageable) {
        Page<Photo> photoPage = photorepository.findAll(pageable);
        return photoPage.map(PhotoResponse::fromEntity);
    }

    @Transactional
    public String deletePhoto(Long photoId, String name) {
        Photo photo = findPhotoById2(photoId);

        if(validateUserName(name, photo.getNickname())){
            for(String imagePath : photo.getPhotoImagePath()) {
                gcpStorageService.deleteImageFromGCP(imagePath);
            }
            photorepository.delete(photo);
            return "삭제 완료";
        }
        else{
            return "삭제 실패";
        }
    }

    @Transactional
    public Page<PhotoResponse> findByUserNickName(String nickname,Pageable pageable) {
        Page<Photo> photo = photorepository.findByUserNickName(nickname,pageable);
        return photo.map(PhotoResponse::fromEntity);
    }

    @Transactional
    public void addLikeCount(Long photoId) {
        Photo photo = findById2(photoId);
        photo.increaseLikeCount();
    }

    @Transactional
    public void deleteLikeCount(Long photoId) {
        Photo photo = photorepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사진이 없습니다."));
        photo.decreaseLikeCount();
    }

    private PhotoResponse findPhotoById(Long photoId) {
        Photo photo = photorepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사진이 없습니다."));
        return PhotoResponse.fromEntity(photo);
    }

    private Photo findPhotoById2(Long photoId) {
        return photorepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사진이 없습니다."));
    }

    public Page<PhotoResponse> findByKakaoId(Long kakaoId, Pageable pageable) {
//        User user = userRepository.findByKakaoId(kakaoId)
//            .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));
//
//        String decodeName = URLDecoder.decode(user.getNickName(), StandardCharsets.UTF_8);
//
//        Page<Photo> photo = photorepository.findByUserNickName(decodeName, pageable);

        Page<Photo> photo = photorepository.findByKakaoId(kakaoId,pageable);

        return photo.map(PhotoResponse::fromEntity);
    }

    public void save(Photo photo) {
        photorepository.save(photo);
    }

    public Page<PhotoResponse> findLikedPhotosByUser(List<Like> likes, Pageable pageable) {
        if (likes.isEmpty()) {
            //logger.error("예외처리에 의한 빈 likes" );
            return Page.empty();
        }

        if(pageable.getPageNumber() < 0){
            //logger.error("page getPageNumber값이 < 0");
            return Page.empty();
        }

        if(pageable.getPageSize() <= 0){
            //logger.error("page getPageSize() <= 0");
            return Page.empty();
        }

        List<Photo> photos = likes.stream()
            .map(Like::getPhoto)
            .collect(Collectors.toList());

        int start = (int) pageable.getOffset();  // 페이지 시작 인덱스
        int end = Math.min((start + pageable.getPageSize()), photos.size());
        // 페이지 끝 인덱스
        Page<Photo> photo = new PageImpl<>(photos.subList(start, end), pageable, photos.size());

        return photo.map(PhotoResponse::fromEntity);
    }

    public Page<PhotoResponse> search(SearchRequest searchRequest, Pageable pageable) throws UnsupportedEncodingException {
        String hairName = searchRequest.getHairName();
        String hairLength = searchRequest.getHairLength();
        String hairColor = searchRequest.getHairColor();
        String gender = searchRequest.getGender();

        // null 체크를 추가하여 디코딩
        String decodeHairName = hairName != null ? URLDecoder.decode(hairName, StandardCharsets.UTF_8) : null;
        String decodeHairLength = hairLength != null ? URLDecoder.decode(hairLength, StandardCharsets.UTF_8) : null;
        String decodeHairColor = hairColor != null ? URLDecoder.decode(hairColor, StandardCharsets.UTF_8) : null;
        String decodeGender = gender != null ? URLDecoder.decode(gender, StandardCharsets.UTF_8) : null;

        Page<Photo> photo = photorepository.search(decodeHairName,decodeHairLength,decodeHairColor,decodeGender,pageable);

        return photo.map(PhotoResponse::fromEntity);
    }

    private boolean validateUserName(String userName, String photoName) {
        if (!photoName.equals(userName)) {
            return false;
        }
        else{
            return true;
        }
    }

    public void updateName(String preName,String name){// 이 기능이제 필요없을 듯?
        //preName으로 게시글들 불러오고 name으로 바꾸기
        List<Photo> photos = photorepository.findByUserNickName(preName);
        for(Photo photo : photos){
            photo.setNickname(name);
        }
    }
}
