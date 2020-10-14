-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(username, password, admin, last_login, is_active)
VALUES
(:username, :password, :admin, CURRENT_TIMESTAMP, true)

-- :name update-user-last-login! :! :n
-- :doc updates the last_login field of the specified user
UPDATE users
SET last_login = CURRENT_TIMESTAMP
WHERE username = :username

-- :name update-user-is-active! :! :n
-- :doc updates the is_active field of the specified user
UPDATE users
SET is_active = :is_active
WHERE username = :username

-- :name get-user :? :1
-- :doc retrieves a user record given the username
SELECT * FROM users
WHERE username = :username

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE username = :username

-- :name create-client! :! :n
-- :doc creates a new client record
INSERT INTO clients
(id, secret, description)
VALUES
(:id, :secret, :description)

-- :name get-client :? :1
-- :doc retrieves a client by client id
SELECT * FROM clients
WHERE id = :id

-- :name delete-client! :! :n
-- :doc deletes a client record given the id
DELETE FROM clients
WHERE id = :id
