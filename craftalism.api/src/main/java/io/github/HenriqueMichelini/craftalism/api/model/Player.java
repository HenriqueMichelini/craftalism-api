package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @NotNull(message = "Player requires a UUID")
    @Column(nullable = false, unique = true)
    private UUID uuid;

    @NotBlank(message = "Player requires a name")
    @Column(nullable = false, unique = true)
    private String name;

    public Player() {}

    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
