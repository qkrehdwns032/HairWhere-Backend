package hairwhere.hairwhere.dto;

import lombok.Getter;

@Getter
public class SearchRequest {
    private String hairName;
    private String hairLength;
    private String hairColor;
    private String gender;
}
