package ca.mcgill.ecse321.gameorganizer.dto.request;

public class GameSearchCriteria {
    private String name;
    private Integer minPlayers;
    private Integer maxPlayers;
    private String category;
    private Double minRating;
    private Boolean available;
    private String ownerId;
    private String sort;
    private String order;

    // Default constructor
    public GameSearchCriteria() {}

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getMinPlayers() { return minPlayers; }
    public void setMinPlayers(Integer minPlayers) { this.minPlayers = minPlayers; }

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
} 