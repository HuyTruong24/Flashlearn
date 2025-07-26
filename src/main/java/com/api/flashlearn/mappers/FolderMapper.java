package com.api.flashlearn.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.flashlearn.dtos.CreateFolderRequest;
import com.api.flashlearn.dtos.FolderDto;
import com.api.flashlearn.dtos.FolderItemDto;
import com.api.flashlearn.entities.Folder;

@Mapper(componentModel = "spring")
public interface FolderMapper {
    FolderDto toDto(Folder folder);
    FolderItemDto toFolderItemDto(Folder folder);
    @Mapping(source = "userId", target = "user.id")
    Folder toEntity(CreateFolderRequest folderDto, Long userId);
}
