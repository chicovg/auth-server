CREATE TABLE users(
    username VARCHAR(20) PRIMARY KEY,
    password VARCHAR(300) NOT NULL,
    admin BOOLEAN DEFAULT false,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);
