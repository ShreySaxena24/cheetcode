CREATE TABLE scheduler_cursors (
    scheduler_name VARCHAR(100) PRIMARY KEY,
    cursor_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questions (
    id SERIAL PRIMARY KEY,
    leetcode_question_id VARCHAR(50) UNIQUE NOT NULL,
    title_slug VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    content_html TEXT,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content_synced_at TIMESTAMP
);

CREATE TABLE solution_videos_context (
    id SERIAL PRIMARY KEY,
    question_id INTEGER REFERENCES questions(id) ON DELETE CASCADE NOT NULL,
    video_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    curated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_video UNIQUE (question_id, video_id)
);

CREATE TABLE knowledge_sources (
    id SERIAL PRIMARY KEY,
    question_id INTEGER REFERENCES questions(id) ON DELETE CASCADE NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    source_title VARCHAR(255),
    author VARCHAR(255),
    raw_content TEXT,
    metadata JSONB,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    scraped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_source_url UNIQUE (question_id, source_type, source_url)
);
