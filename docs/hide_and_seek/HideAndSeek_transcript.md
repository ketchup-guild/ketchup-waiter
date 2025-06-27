[Speaker 1]
All right, here's the PRD for a feature of the catch-up bot, where ketchup is a guild on Discord. Um, what we want to
do, is we want to play a? A

[Speaker 2]
Jet lag. The game inspired hide and seek game. Um, there is already a readme about this.

[Speaker 1]
But. Summarize the ideas.

[Speaker 2]
Okay, so this is the Discord Port using kotlin.

[Speaker 1]
Uh, it's already pre-existing, and it has some framework to integrate with stuff. And. Uh, this whole feature needs to
be implemented using that framework as best as possible. The board needs a few commands, so these are commands that I
want to run through the. Someone who is registered? Actually, no, it can just be anyone.

Anyone should be able to run a command to set up a game in any channel. The games are all Global. They're not limited to
any any individual. A server, but responses are sort of in the channel. And then, if it has to create channels, those
channels will be created in the server that the command ran.

Okay, so there's one command to register players. This should be like all of these commands should also be essentially
only one command in Discord with then multiple sub commands so that registers registering and unregistering them is.

Like slash? Hns for hide and seek, hide and seek. So, slash hns.

Start. To like, like, completely start the flow. Then it should respond with you're. Uh, you can. Now, we're just a
player with Slash hns. Player. And then the like? Whoever it is, right. So, whatever? And that adds the players. Um.

One argument after that, so it's hns. Play. The name of the the mention of the player. Personal Discord, and then the
last argument should be the team, which is just a number. So you go hns? Mtib, which is my username one to add me

[Speaker 2]
To team one.

[Speaker 1]
The. Like name can be anything right. It could be a number it could be text. Okay, so yeah, we make this you add those
pure to the player. You to add all the players to the team. And. Then you can. Slash hn. Shuffle. Which will assign one
team to be the Seekers and the other teams to be.

Uh, sorry. Assign one team to behind us and the other teams to be Seekers, and it will Output the sort of the team like
this team is hiding here. The team members this team seeking here, the team members. You can then go. Hns. Hide, so
slash hns hide. Uh, and this will do a lot.

So, the first of all it will print that, like, yes, I'm starting to set up set things up. Here's the the teams again.
And then it will create new channels. This can just happen, like global, at the top of the guild, because these are
temporary, so it'll create a.

In which it adds. In each channel it? All the hiders, so all the members who were chosen to be hiding. And then it will
add. One or all members of one of the team, one of the seeking teams. So, in what was team, if there's teams a, b, and c
and team a is hiding?

Then, it will create a Channel with Team A and Team B in it, and it'll create another Channel with Team A and Team C in
it. It will then. A post in each of the channels that the hiding period has started. This is configurable in code. How
long it is should default to 30 minutes?

Uh, but uh yeah, make it configurable over environment, so we'll post that. And then the feature will sleep for that
amount of time. In this time, the hiding team gets to go and hide after that hiding period is over. It posts in those
channels again that the seeking teams are now able to start, allowed to start seeking, and are allowed to ask a
question.

And here it will post a link to the rule document that is. Available on GitHub, as well as a summary of the possible
questions that they can ask. Uh, and it'll do that every. Uh, like every 15 minutes is the default, but again, I want
this configurable over the environment, and it'll post like?

A little say it'll say like, you can ask another question. Uh, in, in 15 minutes. Uh, you can do all that. And then it
just keeps on going until. Someone uses slash hns found. And then space, and then the team name. If that is used, then
the. 13 brins.

The team has found the hiding places. The hiding team. It posts in their own channel. Congratulation message. So in the
in the channel that was created for that team, and this the height is in there, it writes. Communiculation T1, you took
this long to find them, right? Timers all over long.

It took between the start of the seeking period, and now and then, in all the other channels. So, in the other. In
relative team, B founder in team seeds Channel, it writes. B has found the hiders. Know, allowed to ask every five
minutes.

So that? Uh, and then the interval for allowing them to ask questions increases, decreases to a shorted amount of time,
which again is configurable through environment where variables. But it's separate from the other interval. It is
shorter interval is. Used for every other team that is still seeking. Uh, once all teams have and you, don't you no
longer remind the team that has already found the hiders they don't have to ask questions anymore, so that's not
necessary.

And then.

Yeah, once everyone has found the team where everyone can use the hns found team name. Once everyone has used that you
post in all the channels that, hey, the game is over. And then you post the ranking of which team like the order of
teams, and when they found.

The hiders and how long it took them. Uh, yes, and. Um. Then you reset everything, so you keep

[Speaker 2]
The channels around.

[Speaker 1]
But you reset everything else, so? All the memory of? Actually, no, you keep it keep having everything around. Just make
sure that if someone? The hns was a shuffle. The thing that I mentioned earlier if someone wants that, that it just
reshuffles, who's the Seeker team, and who's the hiding team?

And make sure that you can't be the. Hiding team tries in a row. So, like, pick, pick a hiding team that hasn't wasn't
just the hiding team. Um. Yeah, and then then it just restart. It should recreate all the channels, or like create new
channels for the new game.

Yes, that's it.

Make sure all the games are numbered so that the number is also included in the channel names. So, like started one and
then count up.

Yes, that's it.