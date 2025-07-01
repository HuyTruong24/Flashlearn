package com.api.flashlearn.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "folder")
@Builder
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "summary")
    private String summary;

    @Column(name = "created_at",  insertable = false, updatable = false)
    private LocalDate createdAt;
    
    @Column(name = "is_favorite")
    private boolean isFavorite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username")
    private User user;

    @OneToMany(mappedBy = "folder", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<Flashcard> flashcards = new ArrayList<>();
}
