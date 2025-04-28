package hairwhere.hairwhere.dto;

import hairwhere.hairwhere.domain.Photo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PhotoResponse {

    private Long id;
    private String nickname;
    private List<String> photoImagePath;
    private int likeCount;
    private String hairName;
    private String text;
    private String gender;
    private LocalDateTime created;
    private String hairSalon;
    private String hairSalonAddress;
    private String hairLength;
    private String hairColor;

    // User 엔티티 대신 필요한 정보만 포함
    private Long kakaoId;
    private String userProfilePath;
    private List<String> likedNickNames;

    public static PhotoResponse fromEntity(Photo photo) {
        PhotoResponse response = new PhotoResponse();

        response.setId(photo.getId());
        response.setNickname(photo.getNickname());
        response.setPhotoImagePath(photo.getPhotoImagePath());
        response.setLikeCount(photo.getLikeCount());
        response.setHairName(photo.getHairName());
        response.setText(photo.getText());
        response.setGender(photo.getGender());
        response.setCreated(photo.getCreated());
        response.setHairSalon(photo.getHairSalon());
        response.setHairSalonAddress(photo.getHairSalonAddress());
        response.setHairLength(photo.getHairLength());
        response.setHairColor(photo.getHairColor());

        // User 정보 설정
        if (photo.getUser() != null) {
            response.setKakaoId(photo.getUser().getKakaoId());
            response.setUserProfilePath(photo.getUser().getProfileImageUrl());
        }

        // 좋아요 한 사용자 이름 목록
        List<String> likedUserNames = photo.getLikes().stream()
            .map(like -> like.getUser().getNickName())
            .collect(Collectors.toList());
        response.setLikedNickNames(likedUserNames);

        return response;
    }
}
