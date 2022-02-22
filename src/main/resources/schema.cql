CREATE KEYSPACE IF NOT EXISTS message_box WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS message_box.conversation
(
    conversation_id  text,
    interlocutors set<uuid>,
    PRIMARY KEY (conversation_id)
);

CREATE TABLE IF NOT EXISTS message_box.message
(
    author_id  uuid,
    time timestamp,
    content text,
    conversation_id text,
    PRIMARY KEY ((conversation_id), author_id, time)
);

