package com.situnes.service;

import com.situnes.domain.TasteProfile;
import com.situnes.domain.User;
import org.springframework.stereotype.Service;

/**
 * Builds the named music profile after a genre's rounds complete.
 * TODO: implement.
 *
 * Archetype matrix (MVP): popularity × era from each song's top-5 by Elo.
 *   popularity >= 70            -> "MAINSTREAM"
 *   popularity < 70             -> "DEEP"
 *   median release year >= 2015 -> "NOW" / "2000S" / "90S" buckets
 * Key example: "HIPHOP_DEEP_90S" -> name "Boom Bap Archaeologist".
 * Keep the name table in a simple Map or enum — content, not ML, for MVP.
 */
@Service
public class TasteProfileService {

    public TasteProfile buildProfile(User user, String genre) {
        throw new UnsupportedOperationException("TODO: rank songs by Elo, derive archetype, persist TasteProfile");
    }
}
