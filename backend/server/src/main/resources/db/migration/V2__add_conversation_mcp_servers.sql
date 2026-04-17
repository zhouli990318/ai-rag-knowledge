CREATE TABLE conversation_mcp_server (
    conversation_id BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    mcp_server_id   BIGINT NOT NULL,
    PRIMARY KEY (conversation_id, mcp_server_id)
);

CREATE INDEX idx_conversation_mcp_server_conversation_id ON conversation_mcp_server(conversation_id);