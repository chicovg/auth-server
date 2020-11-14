CREATE TABLE users(
    id SERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    password VARCHAR(300) NOT NULL,
    admin BOOLEAN DEFAULT false,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    UNIQUE(username)
);
