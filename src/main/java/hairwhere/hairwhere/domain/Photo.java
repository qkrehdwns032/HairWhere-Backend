package hairwhere.hairwhere.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
public class Photo {

    private static final int DEFAULT_LIKE_NUM = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long id;

    @Column(name = "kakaoId")
    private Long kakaoId;

    @Column(name = "nickname")
    private String nickname;

    @ElementCollection
    @CollectionTable(name = "photo_image_paths", joinColumns = @JoinColumn(name = "photo_id"))
    @Column(name = "photo_imagePath")
    @Builder.Default
    private List<String> photoImagePath = new ArrayList<>();

    @Column(name = "like_count")
    @Builder.Default
    private int likeCount = DEFAULT_LIKE_NUM;

    @Column(name = "hair_name")
    private String hairName;

    @Column(name = "text")
    private String text;

    @Column(name = "gender")
    private String gender;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "hairSalon")
    private String hairSalon;

    @Column(name = "hairSalonAddress")
    private String hairSalonAddress;

    @Column(name = "hairLength")
    private String hairLength;

    @Column(name = "hairColor")
    private String hairColor;

    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "photo")
    @JsonIgnore
    @Builder.Default
    private List<Like> likes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount--;
    }

    public Photo(Long id, Long kakaoId,String nickname, List<String> photoImagePath, int likeCount, String hairName, String
        text, String gender, LocalDateTime created, String hairSalon, String hairSalonAddress, String
                     hairLength, String hairColor, List<Comment> comments, List<Like> likes, User user) {
        this.id = id;
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.photoImagePath = photoImagePath;
        this.likeCount = likeCount;
        this.hairName = hairName;
        this.text = text;
        this.gender = gender;
        this.created = created;
        this.hairSalon = hairSalon;
        this.hairSalonAddress = hairSalonAddress;
        this.hairLength = hairLength;
        this.hairColor = hairColor;
        this.comments = comments;
        this.likes = likes;
        this.user = user;
    }
}
