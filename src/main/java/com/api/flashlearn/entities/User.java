package com.api.flashlearn.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;
    
    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "profile_img_url")
    @Builder.Default
    private String profileImgUrl = null;

    @Column(name = "verification_code", length = 6)
    private String verificationCode;
    
    @Column(name = "code_expiry_time")
    private LocalDateTime codeExpiryTime;

    @Column(name = "enabled")
    private boolean isEnabled;

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
