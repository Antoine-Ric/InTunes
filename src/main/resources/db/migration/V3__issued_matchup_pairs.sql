-- Freeze the round target for each attempt; changing config affects new attempts only.
ALTER TABLE matchup_runs ADD COLUMN round_limit integer NOT NULL DEFAULT 16;
ALTER TABLE matchup_runs ADD CONSTRAINT ck_runs_round_limit CHECK (round_limit > 0);
ALTER TABLE matchups ADD CONSTRAINT uq_matchups_vote_run UNIQUE (id, run_id);

CREATE TABLE issued_matchup_pairs (
    id uuid PRIMARY KEY,
    run_id bigint NOT NULL,
    user_id bigint NOT NULL,
    genre varchar(50) NOT NULL,
    song_a_id bigint NOT NULL,
    song_b_id bigint NOT NULL,
    round_number integer NOT NULL CHECK (round_number > 0),
    status varchar(10) NOT NULL CHECK (status IN ('ISSUED','VOTED')),
    issued_at timestamp(6) with time zone NOT NULL,
    resolved_at timestamp(6) with time zone,
    vote_id bigint UNIQUE,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_issued_run_context FOREIGN KEY (run_id, user_id, genre)
        REFERENCES matchup_runs (id, user_id, genre),
    CONSTRAINT fk_issued_song_a FOREIGN KEY (song_a_id, genre) REFERENCES songs (id, genre),
    CONSTRAINT fk_issued_song_b FOREIGN KEY (song_b_id, genre) REFERENCES songs (id, genre),
    CONSTRAINT fk_issued_vote_run FOREIGN KEY (vote_id, run_id) REFERENCES matchups (id, run_id),
    CONSTRAINT ck_issued_distinct CHECK (song_a_id <> song_b_id),
    CONSTRAINT ck_issued_resolution CHECK (
        (status = 'ISSUED' AND vote_id IS NULL AND resolved_at IS NULL) OR
        (status = 'VOTED' AND vote_id IS NOT NULL AND resolved_at IS NOT NULL AND resolved_at >= issued_at)),
    CONSTRAINT uq_issued_run_round UNIQUE (run_id, round_number)
);
CREATE UNIQUE INDEX uq_issued_run_pair ON issued_matchup_pairs
    (run_id, least(song_a_id, song_b_id), greatest(song_a_id, song_b_id));
CREATE UNIQUE INDEX uq_issued_pending_run ON issued_matchup_pairs (run_id) WHERE status = 'ISSUED';
