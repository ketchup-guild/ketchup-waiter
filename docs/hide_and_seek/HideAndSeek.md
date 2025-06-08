# mtib's Hide & Seek

Inspired by Jet Lag: The Game's hide and seek series.

## Overview

This variant of the game is meant to be played in a single afternoon in Copenhagen, with 4–8 people.
It needs to be simple, and ideally make each round take 1–3 hrs.

Components:

- Discord bot
- Rules website

## Discord bot

Bot commands to:

- register players
- register or shuffle teams
- randomly pick hiding team
- start hiding period

Starting the hiding period creates channels for each seeking team which contains them and the hiding team members.
In each team channel the bot will regularly post updates:
The hiding team has 45 minutes to get to their hiding area. The hiding area is defined as 500 meters centered on the
nearest Metro station.
At least one member of each team of seekers must share their location with the hider in the team's channel. Once they
are within 500 meters of the hiders the endgame starts.
During the endgame the hiders are not allowed to move.

- after the hiding period is over and the seekers are allowed to start moving
- every 15 minutes after the hiding period: reminder post that the seekers are allowed to ask a question

Then there is a command to finish the game when a team of seekers have found the hiding team, this posts in all hiding
team channels and now allows all remaining hiding teams to ask questions every 5 minutes

From now on, until all seeking teams have posted that they found the hiders the bot will post the same reminder from
above every five minutes, allowing them to ask many questions.

Then there is a command to randomly pick the next hiding team (out of the teams that have hidden the least so far).

## Questions

After the bot notifies the seeking team channel, the seeking team can ask a question in their channel with the hiders.
The hiders have to respond in the channel the question was asked within 10 minutes after the question was answered or
incur a 30-minute penalty.

### Temperature

- Out of two places the seekers have been in within the last 30 minutes, ask which one is closer to the hider.
    - Example: Out of the Central Station and Trianglen Station, which is closer to you?

### Comparison

- Does the most recently taken transport line service the station or stop the hider is hiding at.
    - Example: Does the M1 service your stop.
- Is the hiding team closer to water than the seekers?
    - Example: We (the seekers) are 300 meters from our closest body of water are you (the hiders)
      closer than that to your closest body of water.
    - This distance is as the crow flies: use "Measure Distance" on Google Maps to determine this accurately.
- Is the hiding team more northern or southern than the hiding team right now?
- Is the hiding team more eastern or western than the hiding team right now?

### Radar

The seekers can pick the size of the radar from the list below, and ask if the hiders are within that distance from the
seekers' current position:

- 500m
- 1km
- 2km
- 3km
- 4km
- 5km

This distance is as the crow flies: use "Measure Distance" on Google Maps to determine this accurately.

### Photo

- Selfie!
    - The hiders have to take a selfie, they are allowed to blur or cover any text in the picture before sending it.
    - Endgame: You may not move, get up or turn around, if there is more than one hider only one of the hiders needs to
      be in the selfie.
- Straight up photo.
    - The hiders have to take a non-zoomed photo pointing straight up.
- The tallest building visible from the train station
    - The hiders have to take a photo of the building with the biggest apparent height visible from any exit of the
      station their hiding zone is centered on. The picture can be zoomed or cropped, but must include two sides of the
      building and the top (i.e. a top corner of the building). Any text may be censored.
- Platform
    - The hiders have to take a photo of the metro station. It must include three visible features (tiles, grafiti,
      signs, architecture, ...). Text may be censored.
- Body of water
    - The hiders have to take a photo of the closest body of water (pond, lake, sea) in their zone (if there is one).
- Street
    - The hiders have to take a photo across one of the streets in their zone (standing on one side of the road, taking
      a picture straight across it), text may be censored.

### Oddball

- First letter of the closest street.
- How long would it get you to get to some place on foot, according to Google Maps?
    - Example: How long would it take you to get to Tivoli by foot according to Google Maps