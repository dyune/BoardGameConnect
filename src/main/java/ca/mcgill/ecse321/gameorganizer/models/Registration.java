package ca.mcgill.ecse321.gameorganizer.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Represents an event registration in the system.
 * Links an attendee (Account) with a specific Event and tracks when the registration was made.
 * 
 * @author @Shine111111
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
public class Registration {

    /** Unique identifier for the registration */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    /** The date when the registration was created */
    private Date registrationDate;

    /** The account of the person attending the event */
    @ManyToOne
    private Account attendee;

    /** The event being registered for */
    @ManyToOne
    private Event eventRegisteredFor;

    /**
     * Creates a new registration with the specified registration date.
     *
     * @param aRegistrationDate The date when the registration is made
     */
    public Registration(Date aRegistrationDate) {
        registrationDate = aRegistrationDate;
    }

    /**
     * Returns a string representation of the Registration.
     *
     * @return A string containing the registration's ID and date
     */
    public String toString() {
        return super.toString() + "[" +
                "id" + ":" + getId() + "]" + System.getProperties().getProperty("line.separator") +
                "  " + "registrationDate" + "=" + (getRegistrationDate() != null ? !getRegistrationDate().equals(this) ? getRegistrationDate().toString().replaceAll("  ", "    ") : "this" : "null") + System.getProperties().getProperty("line.separator");
    }
}
