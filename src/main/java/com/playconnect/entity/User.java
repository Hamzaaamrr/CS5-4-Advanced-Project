package com.playconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    private String role;         
    public User() {}  // Empty constructor (required by JPA) work all frameworks, springboot can create users
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
	
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	public boolean valid() {
		return firstName != null && !firstName.trim().isEmpty()
			&& lastName != null && !lastName.trim().isEmpty()
			&& email != null && !email.trim().isEmpty();
	}
// methods:
	public boolean isAdmin() {
        return "ADMIN".equals(role);  // Checks if user is admin
    }
}
