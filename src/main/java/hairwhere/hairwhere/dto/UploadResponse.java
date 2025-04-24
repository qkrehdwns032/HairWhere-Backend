package hairwhere.hairwhere.dto;

import hairwhere.hairwhere.domain.User;
import lombok.Getter;

@Getter
public class UploadResponse {
    private User user;
    private Long id;
    private String errorMessage;

    public UploadResponse(User user, Long id){
        this.user = user;
        this.id = id;
    }

    public UploadResponse(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

}
