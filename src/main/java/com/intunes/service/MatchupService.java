package com.intunes.service;

import com.intunes.domain.*;
import com.intunes.dto.MatchupPair;
import com.intunes.dto.SongDto;
import com.intunes.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.intunes.service.MatchupException.Reason.*;

/**
 * Run-scoped selection and persisted resume state. Atomic vote recording remains #24.
 */
@Service
public class MatchupService {
    private static final Set<String> GENRES = Set.of("hiphop", "rnb", "afrobeats", "pop", "country", "gospel", "rock");
    private final UserRepository users;
    private final MatchupRunRepository runs;
    private final SongRepository songs;
    private final MatchupRepository votes;
    private final SongRatingRepository ratings;
    private final IssuedMatchupPairRepository issued;
    private final PairingPolicy pairing;
    private final int rounds;
    private final double startingRating;

    public MatchupService(UserRepository users, MatchupRunRepository runs, SongRepository songs,
                          MatchupRepository votes, SongRatingRepository ratings,
                          IssuedMatchupPairRepository issued, PairingPolicy pairing,
                          @Value("${intunes.matchup.rounds-per-genre:16}") int rounds,
                          @Value("${intunes.matchup.elo-start-rating:1500}") double startingRating) {
        if (rounds < 1 || !Double.isFinite(startingRating)) throw new IllegalArgumentException("Invalid matchup configuration");
        this.users = users;
        this.runs = runs;
        this.songs = songs;
        this.votes = votes;
        this.ratings = ratings;
        this.issued = issued;
        this.pairing = pairing;
        this.rounds = rounds;
        this.startingRating = startingRating;
    }

    /** Resume an active attempt or start the first one. Completed attempts require an explicit retake. */
    @Transactional(noRollbackFor = RunCompleteException.class)
    public MatchupPair nextPair(User user, String genre) {
        validateGenre(genre);
        User owner = lockUser(user);
        Optional<MatchupRun> active = runs.findActive(owner, genre);
        MatchupRun run;
        if (active.isPresent()) {
            run = lockRun(owner, active.get().getId());
        } else {
            Optional<MatchupRun> completed = runs.findLatestCompleted(owner, genre);
            if (completed.isPresent()) throw new RunCompleteException(completed.get().getId());
            requirePool(genre);
            run = runs.saveAndFlush(new MatchupRun(owner, genre, rounds));
        }
        return issueOrResume(run);
    }

    /** Explicit run identity keeps stale clients from silently switching to another attempt. */
    @Transactional(noRollbackFor = RunCompleteException.class)
    public MatchupPair nextPair(User user, Long runId) {
        return issueOrResume(lockRun(lockUser(user), runId));
    }

    /** Abandon an unfinished attempt and start fresh, retaining all prior history (ADR-0003). */
    @Transactional
    public Long startNewRun(User user, String genre) {
        validateGenre(genre);
        User owner = lockUser(user);
        requirePool(genre);
        runs.findActive(owner, genre).ifPresent(active -> lockRun(owner, active.getId()).abandon());
        runs.flush();
        return runs.saveAndFlush(new MatchupRun(owner, genre, rounds)).getId();
    }

    /** Completion counts only votes in the current attempt, never combined user history. */
    @Transactional
    public boolean isComplete(User user, String genre) {
        validateGenre(genre);
        User owner = lockUser(user);
        Optional<MatchupRun> active = runs.findActive(owner, genre);
        if (active.isEmpty()) {
            if (runs.findLatestCompleted(owner, genre).isPresent()) return true;
            requirePool(genre);
            return false;
        }
        return completeIfFinished(lockRun(owner, active.get().getId()));
    }

    @Transactional
    public boolean isComplete(User user, Long runId) {
        return completeIfFinished(lockRun(lockUser(user), runId));
    }

    /** Full issued-pair vote validation, Elo updates, and request idempotency are tracked by #24. */
    public void recordResult(User user, Long winnerSongId, Long loserSongId, String genre) {
        throw new UnsupportedOperationException("TODO #24: atomically validate issued pair, record vote, and update Elo");
    }

    private MatchupPair issueOrResume(MatchupRun run) {
        requireActive(run);
        List<Song> pool = requirePool(run.getGenre());
        List<Matchup> history = votes.findByRunOrderByCreatedAtAscIdAsc(run);
        Optional<IssuedMatchupPair> pending = reconcilePending(run, history);
        if (history.size() >= run.getRoundLimit() || pairing.isExhausted(pool, history)) {
            run.complete();
            runs.flush();
            throw new RunCompleteException(run.getId());
        }
        if (pending.isPresent()) {
            if (pending.get().getRoundNumber() != history.size() + 1) {
                throw new MatchupException(PAIR_STATE_CONFLICT, "Pending pair does not match the run's vote count");
            }
            return dto(pending.get());
        }
        List<SongRating> currentRatings = ratings.findByRunAndSongInOrderBySongIdAsc(run, pool);
        PairingPolicy.Pair choice = pairing.select(pool, history, currentRatings, history.size() + 1, startingRating)
                .orElseThrow(() -> new IllegalStateException("Exhaustion and pairing eligibility disagree"));
        return dto(issued.saveAndFlush(new IssuedMatchupPair(run, choice.songA(), choice.songB(), history.size() + 1)));
    }

    private boolean completeIfFinished(MatchupRun run) {
        if (run.getStatus() == MatchupRun.Status.COMPLETED) return true;
        requireActive(run);
        List<Song> pool = requirePool(run.getGenre());
        List<Matchup> history = votes.findByRunOrderByCreatedAtAscIdAsc(run);
        reconcilePending(run, history);
        if (history.size() >= run.getRoundLimit() || pairing.isExhausted(pool, history)) {
            run.complete();
            return true;
        }
        return false;
    }

    /** Reconcile votes already saved by another transaction before resuming an attempt. */
    private Optional<IssuedMatchupPair> reconcilePending(MatchupRun run, List<Matchup> history) {
        Optional<IssuedMatchupPair> pending = issued.findPendingByRun(run);
        if (pending.isEmpty()) return pending;
        IssuedMatchupPair pair = pending.get();
        PairingPolicy.PairKey key = PairingPolicy.PairKey.of(pair.getSongA(), pair.getSongB());
        Optional<Matchup> recorded = history.stream().filter(vote ->
                key.equals(PairingPolicy.PairKey.of(vote.getSongA(), vote.getSongB()))).findFirst();
        if (recorded.isPresent()) {
            pair.markVoted(recorded.get());
            issued.flush(); // Resolve the partial unique key before issuing the next pair.
            return Optional.empty();
        }
        return pending;
    }

    private User lockUser(User user) {
        if (user == null || user.getId() == null) throw new MatchupException(INVALID_USER, "A persisted user is required");
        return users.findByIdForUpdate(user.getId()).orElseThrow(() -> new MatchupException(INVALID_USER, "User does not exist"));
    }

    private MatchupRun lockRun(User owner, Long runId) {
        if (runId == null) throw new MatchupException(RUN_NOT_FOUND, "Run does not belong to this user");
        return runs.findByIdAndUserForUpdate(runId, owner)
                .orElseThrow(() -> new MatchupException(RUN_NOT_FOUND, "Run does not belong to this user"));
    }

    private void validateGenre(String genre) {
        if (genre == null || !GENRES.contains(genre)) throw new MatchupException(INVALID_GENRE, "Unsupported genre");
    }

    private List<Song> requirePool(String genre) {
        // Missing previews do not exclude songs (issue #20).
        List<Song> pool = songs.findByGenre(genre);
        if (pool.size() < 2) throw new MatchupException(INSUFFICIENT_SONGS, "Genre needs at least two songs to start a matchup");
        return pool;
    }

    private void requireActive(MatchupRun run) {
        if (run.getStatus() == MatchupRun.Status.COMPLETED) throw new RunCompleteException(run.getId());
        if (run.getStatus() != MatchupRun.Status.IN_PROGRESS) throw new MatchupException(RUN_ABANDONED, "Run was abandoned");
    }

    private MatchupPair dto(IssuedMatchupPair pair) {
        return new MatchupPair(songDto(pair.getSongA()), songDto(pair.getSongB()), pair.getRoundNumber(),
                pair.getRun().getRoundLimit(), pair.getRun().getId(), pair.getId());
    }

    private SongDto songDto(Song song) {
        return new SongDto(song.getId(), song.getTitle(), song.getArtist(), song.getAlbumArtUrl(), song.getPreviewUrl());
    }
}
