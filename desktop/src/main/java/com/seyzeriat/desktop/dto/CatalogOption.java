package com.seyzeriat.desktop.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A pickable genre / platform / company option, with the id + display name.
 * Equality is by id so {@link javafx.scene.control.ListView} selection works
 * across reloads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogOption {
    private String id;
    private String name;

    public CatalogOption() {}

    public CatalogOption(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogOption that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
