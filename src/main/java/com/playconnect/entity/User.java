package Entity;  

import jakarta.persistence.*;  // JPA annotations (bdl ma akteb SQL)
import java.time.LocalDateTime;  // lel dates and times (zay emta el user registered)

@Entity  // This class is a database table
@Table(name = "users")  // Table name: users
public class User {    
    
    @Id  // PRIMARY KEY (unique ID for each user)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Database auto-generates ID (1,2,3...)
    private Long id;  // Unique user number (ex: 101)
    // el user's firstname 
    private String firstName;  
    private String lastName;   
    
 // maynfa3sh two users can have same username
    @Column(unique = true)  
 // Login username 
    private String username; 
    
 // No two users can have same email
    @Column(unique = true)  
    private String email;  
    
    private String password;  // User's password
    private String role;  // What user can do: PLAYER or ADMIN
    private boolean isActive;  // Can user login? true = yes, false = banned
    
    // Empty constructor: JPA needs this to create User objects
    public User() {}
    
//setter and getters:    
    public String getFullName(){  
        return firstName + " " + lastName;
    }
    
    public Long getId(){
    	return id;
    	} 
    
    public void setId(Long id){ 
    	this.id = id;
    	} 
    
    public String getFirstName(){
    	return firstName; 
    	}  
    
    public void setFirstName(String firstName){
    	this.firstName = firstName; 
    	}  
    
    public String getLastName(){ 
    	return lastName; 
    	} 
    
    public void setLastName(String lastName){
    	this.lastName = lastName;
    	}  
    
    public String getUsername(){
    	return username; 
    	} 
    
    public void setUsername(String username){
    	this.username = username;
    	}
    
    
    public String getEmail(){
    	return email; 
    	}  
    
    public void setEmail(String email){
    	this.email = email;
    	}  
    
    public String getPassword(){
    	return password; 
    	} 
    
    public void setPassword(String password){
    	this.password = password;
    	} 
    
 // Get role (PLAYER or ADMIN)
    public String getRole(){
    	return role; 
    	}  
    
    public void setRole(String role){
    	this.role = role; 
    	} 
    
 // Is account active?
    public boolean isActive(){ 
    	return isActive;
    	}  
    
 // Activate/deactivate
    public void setActive(boolean isActive){
    	this.isActive = isActive; 
    	}  
    
    // bet check lw el user ADMIN 
    public boolean isAdmin() {  
        return "ADMIN".equalsIgnoreCase(role);  // True if role = "ADMIN"
    }
}
