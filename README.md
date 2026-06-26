# CS-360-Portfolio-InventoryApp

CS 360 portfolio submission. A user authenticated inventory management app for Android.

## About this repository

This repository contains the completed app code from Project Three of CS 360 Mobile Architecture and Programming at SNHU. The ZIP file in this repo includes the finalized UI design that was originally built in Project Two and then extended with full backend functionality in Project Three.

## Reflection

### Briefly summarize the requirements and goals of the app you developed. What user needs was this app designed to address?

The assignment was to build a working inventory management app for Android. The idea was to give users a simple way to track items in a collection. I based mine on ManaBox, which I actually use to keep track of my Magic: The Gathering cards. So the user need I was solving was pretty close to home. People who collect things want a fast way to see what they have, add new items, and get a reminder when something runs low. The app needed a login system so each person had their own private inventory, a grid view to actually browse the items, and SMS alerts so the user could get a text when a quantity dropped below a set threshold.

### What screens and features were necessary to support user needs and produce a user centered UI for the app? How did your UI designs keep users in mind? Why were your designs successful?

I had a login screen, an inventory grid screen, an item detail screen for adding or editing items, and a settings screen where the user enters the phone number for SMS alerts. The grid uses a RecyclerView in two columns so users can scan their inventory at a glance instead of scrolling through a long list. I went with a dark Material Design theme because most of the apps I actually use look that way and it is easier on the eyes for late night collection checking. I kept the buttons big and labeled them with text so nothing was hidden behind icons you have to guess at. I think the designs worked because every screen had one clear job and the user always knew what tapping something would do.

### How did you approach the process of coding your app? What techniques or strategies did you use? How could those techniques or strategies be applied in the future?

I worked one piece at a time and made sure the build was green before moving on. I started with the Gradle setup and dependencies, then the database helper class, then the login activity, then the inventory grid, and so on. Every step I built and ran the app to confirm nothing else broke. That kept the problems small. If something failed, I knew it had to be from the last thing I added, not some mystery from three steps back. I want to keep using that approach in future projects because it saved me from the kind of debugging session where you have no idea where the bug is hiding.

### How did you test to ensure your code was functional? Why is this process important, and what did it reveal?

For each feature I ran the app on the emulator and walked through the flow myself. For login I tested creating a new account, logging in with the correct password, logging in with the wrong password, and trying to create a duplicate username. For the inventory grid I added items, edited them, deleted them, and confirmed they stayed tied to the right user when I logged out and back in. For SMS I tested both permission paths since the rubric specifically called out that the app still has to work even if the user denies the permission. Testing matters because the build compiling does not mean the app works. The compiler will happily let you ship a screen that crashes the second you tap a button. Walking through the actual flows showed me where my logic was off in ways I would not have caught just by reading the code.

### Consider the full app design and development process from initial planning to finalization. Where did you have to innovate to overcome a challenge?

The trickiest part was making sure each user only saw their own inventory. The database tables had to be designed so that the inventory items were linked to a user ID, and every query in the app had to filter by the user who was logged in. I had to think through how the app remembered the active user across activities so the right ID got passed into the database calls. Once I worked out a pattern for that, the rest of the CRUD operations were easier because they all followed the same shape.

### In what specific component of your mobile app were you particularly successful in demonstrating your knowledge, skills, and experience?

I think the database layer is where I showed the most. The DatabaseHelper class handles user accounts with hashed passwords, full CRUD for inventory items, and the per user ownership I mentioned above. That class ties almost everything in the app together, and I built it so the rest of the code could call simple methods without having to know what the SQL underneath looked like. Getting that part right is what made the other features possible.
