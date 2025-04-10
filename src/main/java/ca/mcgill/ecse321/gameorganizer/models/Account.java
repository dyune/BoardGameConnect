package ca.mcgill.ecse321.gameorganizer.models;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Represents a user account in the game organization system.
 * This is the base class for all types of user accounts.
 *
 * @author @dyune
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "resetPasswordToken", "resetPasswordTokenExpiry"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;


    @Column(unique = true)
    private String resetPasswordToken;

    private LocalDateTime resetPasswordTokenExpiry;
    // Methods

    /**
     * Creates a new Account with the specified details.
     *
     * @param aName     The display name of the account holder
     * @param aEmail    The email address associated with the account (unique identifier)
     * @param aPassword The password for account authentication
     */
    public Account(String aName, String aEmail, String aPassword) {
        name = aName;
        email = aEmail;
        password = aPassword;
    }

    /**
     * Performs cleanup operations when deleting the account.
     */
    public void delete() {
    }

    /**
     * Returns a string representation of the Account.
     *
     * @return A string containing the account's ID, name, email, and password
     */
    public String toString() {
        return super.toString() + "[" +
                "id" + ":" + getId() + "," +
                "name" + ":" + getName() + "," +
                "email" + ":" + getEmail() + "," +
                "password" + ":" + getPassword() + "]";
    }
}
