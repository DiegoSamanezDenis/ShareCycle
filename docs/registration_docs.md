## changes made : 
rename the sql files in src/main/resources/db/migration/V2_create_users.sql ->  V2__create_users.sql 
	it has a double underscore after V2 which is the convention for flyway to run it for docker, otherwise it gets ignored 
	
	
	## Structure

### Entities
- **User**
  - Base entity for all users.
  - Fields: `id`, `fullName`, `streetAddress`, `email`, `username`, `passwordHash`, `role`, `paymentMethodToken`, `createdAt`, `updatedAt`.
- **Rider** (extends User)
  - Role discriminator: `"RIDER"`.
  - Field: `paymentMethodToken`.
- **Operator** (extends User)
  - Role discriminator: `"OPERATOR"`.

### Repository
- **UserRepository** (interface)
  - Methods:
    - `existsByEmail(String email)` — checks if email already exists.
    - `existsByUsername(String username)` — checks if username already exists.
    - `save(User user)` — saves a user entity to the database.
- **JpaUserRepository** (implementation using JPA/Hibernate)
  - Connects repository methods to the database.

### Services
- **PasswordHasher** (interface)
  - Method: `hash(String plainPassword)` — hashes a password.
- **BcryptHasher** (implements PasswordHasher)
  - Uses BCrypt to hash passwords before storing.

### Use Cases
- **RegisterRiderUseCase**
  - Handles Rider registration.
  - Validates email, username, and password.
  - Checks uniqueness via UserRepository.
  - Hashes password with PasswordHasher.
  - Persists Rider using UserRepository.
  - Returns Rider profile with password hidden.

## Database
- Uses MySQL.
- Discriminator column used to differentiate between Rider and Operator in `users` table.
- Passwords stored as BCrypt hashes in `password_hash` column.