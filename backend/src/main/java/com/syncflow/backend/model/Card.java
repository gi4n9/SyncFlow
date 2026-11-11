package com.syncflow.backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cards", indexes = @Index(name = "idx_cards_list_position", columnList = "list_id, position"))
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private BoardList boardList;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Double position;

    @Column(name = "due_date")
    private Instant dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CardPriority priority = CardPriority.MEDIUM;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "card_assignees",
        joinColumns = @JoinColumn(name = "card_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignees = new HashSet<>();

    protected Card() {}

    public Card(BoardList boardList, String title, String description, Double position,
                Instant dueDate, CardPriority priority, User createdBy) {
        this.boardList = boardList;
        this.title = title;
        this.description = description;
        this.position = position;
        this.dueDate = dueDate;
        this.priority = priority;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public BoardList getBoardList() { return boardList; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Double getPosition() { return position; }
    public Instant getDueDate() { return dueDate; }
    public CardPriority getPriority() { return priority; }
    public long getVersion() { return version; }
    public User getCreatedBy() { return createdBy; }
    public Set<User> getAssignees() { return assignees; }
    public void update(String title, String description, Instant dueDate, CardPriority priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
    }
    public void moveTo(BoardList destination, double position) {
        this.boardList = destination;
        this.position = position;
    }
    public void setAssignees(Set<User> assignees) { this.assignees = assignees; }
    public void setPosition(double position) { this.position = position; }
}
