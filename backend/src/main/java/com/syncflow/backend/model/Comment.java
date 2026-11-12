package com.syncflow.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments", indexes = @Index(name = "idx_comments_card_created_id", columnList = "card_id, created_at, id"))
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "edited_at")
    private Instant editedAt;

    protected Comment() {}

    public Comment(Card card, User author, String content) {
        this.card = card;
        this.author = author;
        this.content = content;
    }

    public Long getId() { return id; }
    public Card getCard() { return card; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }
    public void edit(String content) { this.content = content; this.editedAt = Instant.now(); }
}
