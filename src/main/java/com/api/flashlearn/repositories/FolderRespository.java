package com.api.flashlearn.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.api.flashlearn.entities.Folder;

public interface FolderRespository extends JpaRepository<Folder, Long>{
    @Query("SELECT f FROM Folder f WHERE f.user.username = :username")
    List<Folder> findFoldersByUsername(String username);
}
