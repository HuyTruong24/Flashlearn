package com.api.flashlearn.dtos;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String profileImgUrl;
}
