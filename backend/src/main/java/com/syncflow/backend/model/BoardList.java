package com.syncflow.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_lists", indexes = @Index(name = "idx_board_lists_board_position", columnList = "board_id, position"))
public class BoardList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Double position;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    protected BoardList() {}

    public BoardList(Board board, String name, double position) {
        this.board = board;
        this.name = name;
        this.position = position;
    }

    public Long getId() { return id; }
    public Board getBoard() { return board; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPosition() { return position; }
    public void setPosition(Double position) { this.position = position; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
