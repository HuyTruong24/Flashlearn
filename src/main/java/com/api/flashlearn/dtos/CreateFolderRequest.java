package com.api.flashlearn.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateFolderRequest {
    private String name;
    @JsonProperty("fileUrl")
    private String fileUrl;
    private String summary;
}
