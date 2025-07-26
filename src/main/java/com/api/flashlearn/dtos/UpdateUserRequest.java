package com.api.flashlearn.dtos;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private Long id;
    private String profileImgUrl;
}
