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

    /**
     * Default constructor for Jackson.
     */
    public CatalogOption() {}

    /**
     * Constructs a CatalogOption with a specific ID and name.
     * @param id the option ID
     * @param name the option name
     */
    public CatalogOption(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the option ID.
     * @return the option ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the option ID.
     * @param id the option ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the option name.
     * @return the option name
     */
    public String getName() { return name; }
    
    /**
     * Sets the option name.
     * @param name the option name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns a string representation of the option.
     * @return the option name or an empty string if null
     */
    @Override
    public String toString() {
        return name == null ? "" : name;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Equality is determined by comparing IDs.
     * @param o the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogOption that)) return false;
        return Objects.equals(id, that.id);
    }

    /**
     * Returns a hash code value for the object based on its ID.
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
