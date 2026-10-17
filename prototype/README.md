# InTunes UI prototype

Clickable static mockup of the full MVP flow, exported from the design prototype:

`index.html` — open it in a browser, no server needed.

**Pages (one file, JS view-switching):**
1. Home — hero + "find your sound" CTA
2. Genre select — 5 seeded genres
3. Matchup arena — playable 6-round hip-hop demo, live taste-lean readout, profile reveal
4. Music profile — archetype card ("Boom Bap Archaeologist"), Elo-ranked songs, pinned comments
5. Community board — composer, likes, expandable replies, pin-to-profile
6. Explore — 8 archetype communities with genre filters

Plus a flow map at the top showing how the pages connect, including the loop
where pinned board comments land back on the profile.

**Look:** dark, vinyl-inspired, lime/coral accents.

**How this maps to the backend (`mvp-scaffold` branch):**
each prototype view corresponds to a Thymeleaf template in
`src/main/resources/templates/` (index, genres, matchup, profile, community,
explore). Use this prototype as the visual reference when building those pages
out for real.
