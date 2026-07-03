# Player Interpolation

A runelite plugin that makes local player movement more responsive.

By default, the player's visible model can lag behind their true tile. In some situations, it can be multiple ticks behind. This makes precise movement notoriously difficult without some sort of true tile indicator.

This plugin makes the player's model always closely align to their true tile. It still uses smooth movement (unlike plain old true tile indicators), but movement smoothing is much tighter than default. By the end of a tick, this plugin makes it so that the player's model will be at their true tile location.

## FAQ

1. **Question**: How does this work with animation stalls? **Answer**: It should move the player as if they were unstalled. The animation won't change, but the position will.
2. **Question**: Can NPCs or other players get more responsive movement too? **Answer**: It wouldn't make sense to, since their click zones wouldn't align with the model. And we can't move the click zones because that would violate Jagex's guidelines. The reason we can do this for the player is because the player model doesn't have any click zones associated with it.
3. **Question**: The camera still lags behind a bit, following the old player positions. Can the camera be made more responsive too? **Answer**: I don't think so. Moving the camera that way may violate Jagex's guidelines, where they ban plugins that "offer world interaction in any detached camera mode." There is an option to show an outline (and keep the old player model positioning visible) that makes the camera look less jarring, though.
4. **Question**: Overheads (ie. healthbar, prayers) use the old player positioning. Can this be fixed? **Answer**: Probably. I haven't looked into it yet. The outline option should help with this if you're bothered by it.
5. **Question**: Why do the animations continue to run when I've stopped moving? **Answer**: I didn't want to set animations because that's a whole can of worms. It should be possible, but there are a lot of quirks and edge cases to consider. I'd rather accept a little jank to keep this plugin relatively simple and more robust.
6. **Question** I don't care about smooth movement. I want the character to only show on the true tile, without interpolation. Can I do this? **Answer**: In the configuration, set the duration to 0ms. You can also set the rotation time to 0ms if you want rotations to be instant too.
7. **Question** Should I use this instead of a true tile plugin? **Answer**: This isn't necessarily meant to replace true tiles. This is mostly an experiment to see how tighter movement smoothing would look, and it's likely a little bit buggy in some edge cases, so I'd definitely fiddle around with it before disabling true tiles. They work perfectly fine together though.
