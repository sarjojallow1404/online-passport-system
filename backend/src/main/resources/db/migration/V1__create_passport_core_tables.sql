CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'APPLICANT',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role
        CHECK (role IN ('APPLICANT', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE applicant_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    place_of_birth VARCHAR(150),
    gender VARCHAR(30),
    nationality VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    city_or_town VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    region VARCHAR(100),
    country VARCHAR(100) NOT NULL DEFAULT 'The Gambia',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_applicant_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE passport_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_number VARCHAR(40) NOT NULL,
    applicant_id BIGINT NOT NULL,
    passport_type VARCHAR(40) NOT NULL DEFAULT 'ORDINARY',
    application_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    submission_date TIMESTAMP NULL,
    decision_date TIMESTAMP NULL,
    decision_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_applications_number UNIQUE (application_number),
    CONSTRAINT fk_applications_applicant
        FOREIGN KEY (applicant_id) REFERENCES users(id),
    INDEX idx_applications_applicant (applicant_id),
    INDEX idx_applications_status (application_status)
) ENGINE=InnoDB;

CREATE TABLE application_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    document_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    reviewer_id BIGINT NULL,
    review_note TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_documents_application
        FOREIGN KEY (application_id)
        REFERENCES passport_applications(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_documents_reviewer
        FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_documents_application (application_id),
    CONSTRAINT chk_documents_size CHECK (file_size_bytes > 0)
) ENGINE=InnoDB;

CREATE TABLE application_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    previous_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    changed_by BIGINT NULL,
    change_note TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_status_history_application
        FOREIGN KEY (application_id)
        REFERENCES passport_applications(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user
        FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_status_history_application (application_id)
) ENGINE=InnoDB;
