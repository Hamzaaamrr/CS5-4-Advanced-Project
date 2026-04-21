package Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity  // Makes this class a database table
@Table(name = "users")  // Names the table "users" in database
public class User {    
    @Id  // Marks this as PRIMARY KEY (unique identifier)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increments ID (1,2,3...)
    private Long id;
    private String firstName;
    private String lastName;
    @Column(unique = true) //Unique constraint for username
    private String username;   
    @Column(unique = true)  // Ensures no two users have same email
    private String email;    
    private String password;     
    private String phoneNumber;   
    private String role;         
    private LocalDateTime createdAt;  // When user was created
    private LocalDateTime updatedAt;  // When user was last updated
    public User() {}  // Empty constructor (required by JPA) work all frameworks, springboot can create user
    @PrePersist  // Runs automatically BEFORE saving to database
    protected void onCreate() {
        createdAt = LocalDateTime.now();  // Set creation time
        updatedAt = LocalDateTime.now();  // Set update time
    }
    @PreUpdate  // Runs automatically BEFORE updating in database
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();  // Update the timestamp
    }
    //getters and setters:
    public String getFullName() {
        return firstName + " " + lastName;  // Combines first + last name
    }
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
// methods:
	public boolean isAdmin() {
        return "ADMIN".equals(role);  // Checks if user is admin
    }
}