CREATE KEYSPACE IF NOT EXISTS message_box WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS message_box.conversation
(
    conversation_id uuid,
    interlocutors   set<uuid>,
    last_activity   timestamp,
    PRIMARY KEY (conversation_id)
);

CREATE TABLE IF NOT EXISTS message_box.conversation_by_user
(
    user_id         uuid,
    conversation_id uuid,
    last_activity   timestamp,
    PRIMARY KEY ((user_id), conversation_id)
);

CREATE TABLE IF NOT EXISTS message_box.message
(
    author_id       uuid,
    time            timestamp,
    content         text,
    conversation_id uuid,
    PRIMARY KEY ((conversation_id), author_id, time)
);

