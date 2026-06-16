package com.back.livestream.service;

import com.back.livestream.model.dto.response.AdminLivestreamResponseDTO;
import com.back.livestream.model.enums.LivestreamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminLivestreamService {
    Page<AdminLivestreamResponseDTO> listLivestreams(String keyword, LivestreamStatus status, Pageable pageable);

    AdminLivestreamResponseDTO endLivestream(Long livestreamId);
}
