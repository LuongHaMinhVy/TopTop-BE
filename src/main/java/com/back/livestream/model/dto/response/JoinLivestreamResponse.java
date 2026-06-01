package com.back.livestream.model.dto.response;

import com.back.livestream.model.enums.ParticipantRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinLivestreamResponse {
    private Long livestreamId;
    private String roomName;
    private String livekitUrl;
    private String token;
    private ParticipantRole role;
}
