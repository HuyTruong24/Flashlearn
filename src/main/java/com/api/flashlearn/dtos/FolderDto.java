package com.api.flashlearn.dtos;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FolderDto {
    private Long id;
    private String name;
    @JsonFormat(pattern = "dd MMM yyyy, h:mm a")
    private LocalDateTime createdAt;
    @JsonProperty("isFavorite")
    private boolean isFavorite;
}
