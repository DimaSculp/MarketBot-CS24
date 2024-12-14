Announcement bot for UrFU Flea Market

Authors: Andrey Babenko (https://t.me/minofprop), Dmitry Kukhtey (https://t.me/sculp2ra).

Brief description: Telegram bot that accepts applications for posting ads, sends them for moderation, and also posts them in the UrFU Flea Market. The bot also has the ability to maintain a user's personal account.

Goals:

-implementation of the simplest information commands

implementation of accepting ads and sending them to the moderation chat

-implementation of posting ads confirmed by moderation to the ad channel

-implementation of a personal account with statistics

-implementation of deleting irrelevant ads from the ad channel (optional)

-implementation of currency conversion in the ad (optional)

Development stages:

1.1) Connecting a library for working with Telegram API (https://github.com/pengrad/java-telegram-bot-api);

1.2) Implementation of processing the simplest information commands (/help, /info, /authors, /start) through separate classes;

1.3) Writing tests for the command handler;

1.4) Adding asynchronicity;

2.1) Connecting the DB to the bot;

2.2) Creating a simple personal account (user ID, contact, number of active ads, money earned from ads);

3.1) Creating an ad creation button;

3.2) Creating an ad in several stages: name, description, price, photo (up to 10);

3.3) Parsing the sent information into a beautiful ad;

4.1) Sending a compiled announcement to the moderation chat;

4.2) Adding the possibility of confirmation or refusal with a note of the reason for the publication of the announcement. The response to the message with the announcement with the text "approve" confirms and publishes the announcement in the channel with announcements, any other text is transmitted to the creator of the announcement in the bot as an explanation of the refusal, the announcement is not published;

5.1) Adding saving of multiple ads as links to messages in the database;

5.2) Implementation of displaying the user's ad list;

5.3) Implementation of closing the ad as deletion from the ad channel and from the user's ad list;
