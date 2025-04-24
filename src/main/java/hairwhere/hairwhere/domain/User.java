package hairwhere.hairwhere.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "kakaoId")
    private Long kakaoId;

    @Column(name = "nickName")
    private String nickName;

    @Column(name = "profileImageUrl")
    private String profileImageUrl;

    @Builder
    public User(Long kakaoId, String nickName, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.nickName = nickName;
        this.profileImageUrl = profileImageUrl;
    }

}
