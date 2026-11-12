package com.syncflow.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "labels", uniqueConstraints = @UniqueConstraint(name = "uk_labels_board_name", columnNames = {"board_id", "name"}))
public class Label {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 24)
    private String color = "violet";

    protected Label() {}
    public Label(Board board, String name) { this.board = board; this.name = name; }
    public Long getId() { return id; }
    public Board getBoard() { return board; }
    public String getName() { return name; }
    public String getColor() { return color; }
}
