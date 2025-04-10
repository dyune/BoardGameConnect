package ca.mcgill.ecse321.gameorganizer.dto.request;

/**
 * Data Transfer Object for authentication.
 * Contains email, password, and rememberMe fields.
 * 
 * @author Shayan
 */
public class AuthenticationDTO {

    private String email;
    private String password;
    private boolean rememberMe;

    /**
     * Default constructor.
     */
    public AuthenticationDTO() {
    }

    /**
     * Constructs an AuthenticationDTO with the specified email and password.
     * 
     * @param email the email address
     * @param password the password
     */
    public AuthenticationDTO(String email, String password) {
        this.email = email;
        this.password = password;
        this.rememberMe = false; // Default to false
    }

    /**
     * Constructs an AuthenticationDTO with the specified email, password, and rememberMe flag.
     * 
     * @param email the email address
     * @param password the password
     * @param rememberMe whether to remember the user
     */
    public AuthenticationDTO(String email, String password, boolean rememberMe) {
        this.email = email;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    /**
     * Returns the email address.
     * 
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     * 
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns whether to remember the user.
     * 
     * @return true if the user should be remembered, false otherwise
     */
    public boolean isRememberMe() {
        return rememberMe;
    }

    /**
     * Sets whether to remember the user.
     * 
     * @param rememberMe true if the user should be remembered, false otherwise
     */
    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}