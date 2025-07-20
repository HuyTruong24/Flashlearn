package com.api.flashlearn.dtos;

import lombok.Data;

@Data
public class CreateFolderRequest {
    private String name;
    private String fileUrl;
    private String summary;
}
