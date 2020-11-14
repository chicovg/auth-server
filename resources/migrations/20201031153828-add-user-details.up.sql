CREATE TABLE user_details (
    user_id INT PRIMARY KEY NOT NULL,
    first_name VARCHAR(300),
    last_name VARCHAR(300),
    email VARCHAR(300),
    address VARCHAR(300),
    city VARCHAR(300),
    state VARCHAR(300),
    zipcode VARCHAR(10),
    CONSTRAINT fk_users
        FOREIGN KEY(user_id)
            REFERENCES users(id)
            ON DELETE CASCADE
);
