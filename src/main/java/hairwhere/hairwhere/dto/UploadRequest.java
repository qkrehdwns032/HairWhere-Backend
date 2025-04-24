package hairwhere.hairwhere.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@AllArgsConstructor
public class UploadRequest {

    private String text;
    private String hairName;
    private String gender;
    private String createdStr;
    private String hairSalon;
    private String hairSalonAddress;
    private String hairLength;
    private String hairColor;
    private MultipartFile[] image;
}
