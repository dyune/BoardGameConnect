# READ ME
  
## Database Configuration
- port: 5433
- password: skibidi

## Project Scope
### Functional Scope
- User authentication and role-based access control
-  Game collection management for game owners
-  Event creation and event registration system
-   Borrow requests

### Technical Scope
- Java Spring Boot backend
- PostgreSQL database
- RESTful API architecture
- Gradle build system
- Comprehensive tests (JUnit)
- Development tasks and sprint planning managed through GitHub Issues


## Email Configuration

For the password reset functionality to work, you need to configure email credentials in your environment variables or in a `.env` file.

1. Copy the `.env.example` file to a new file named `.env` 
2. Update the email credentials in the `.env` file:
   ```
   EMAIL_USERNAME=your-email@gmail.com
   EMAIL_PASSWORD=your-app-password
   ```

**Note for Gmail users:** If you're using Gmail and have 2-Step Verification enabled, you'll need to generate an App Password:
1. Go to your Google Account settings
2. Navigate to Security → App passwords
3. Generate a new app password for the application
4. Use this password in the `.env` file instead of your regular password

The system will use these credentials to send password reset emails without requiring manual authentication through the console.

## JWT Configuration

The application uses JSON Web Tokens (JWT) for authentication. You need to configure a secure secret key:

1. In your `.env` file, set a strong secret key that is at least 64 bytes (512 bits) long:
   ```
   JWT_SECRET=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZab
   ```

**Important:** 
- The JWT secret must be at least 64 bytes long as it's using the HS512 algorithm.
- The JWT secret must NOT contain hyphens or special characters that are invalid in Base64 encoding. Use only letters, numbers, and underscores.

## Commit Rules
```<type of commit>: <short description of feature(s) affected/what was done>```

* ```Ex: Feature: Implementing email sending to customers upon successful cart checkout```
* ```Ex: Fix: Fixed bug where Bogo sort would not run in O(n!)```
* ```Ex: Mtn: Refactored code to make it more readable```
