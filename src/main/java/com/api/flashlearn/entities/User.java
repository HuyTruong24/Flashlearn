package com.api.flashlearn.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import lombok.NoArgsConstructor;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {
    @Id
    @Column(name = "username", length = 20)
    private String username;

    @Column(name = "profile_img_url")
    private String profileImgUrl;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @Builder.Default
    private List<Folder> folders = new ArrayList<>();

    public void addFolder(Folder folder) {
        folders.add(folder);
        folder.setUser(this);
    }

    public void removeFolder(Folder folder) {
        folders.remove(folder);
        folder.setUser(this);
    }
}
