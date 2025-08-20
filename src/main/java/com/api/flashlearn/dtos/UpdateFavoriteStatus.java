package com.api.flashlearn.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class UpdateFavoriteStatus {
    @JsonProperty("isFavorite")
    private boolean isFavorite;
}
