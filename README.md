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

1.1) Connecting a library for working with Telegram API (https://github.com/pengrad/java-telegram-bot-api)

1.2) Implementation of processing the simplest information commands (/help, /info, /authors, /start) through separate classes

1.3) Writing tests for the command handler

1.4) Adding asynchronicity

2.1) Connecting the DB to the bot

2.2) Creating a simple personal account (user ID, contact, number of active ads, money earned from ads)
