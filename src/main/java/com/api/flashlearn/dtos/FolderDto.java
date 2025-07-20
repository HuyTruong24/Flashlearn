package com.api.flashlearn.dtos;


import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FolderDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private boolean isFavorite;
}
