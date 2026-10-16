# InventoryApp

An Android inventory app I originally built for CS 360 Mobile Architecture and Programming at SNHU and then reworked for my CS 499 capstone. Each user logs in to their own private inventory, browses it in a grid, searches and sorts it, and can get a text message when an item runs low.

My CS 499 ePortfolio, with the code review video and the narrative for each enhancement, is at https://ncperry95.github.io/

## Versions

The main branch has the finished version. Each stage of the project is tagged so you can browse the code exactly as it was at that point.

* [original_cs360](https://github.com/ncperry95/CS-360-Portfolio-InventoryApp/tree/original_cs360): the app as I turned it in for CS 360
* [enhancement_one](https://github.com/ncperry95/CS-360-Portfolio-InventoryApp/tree/enhancement_one): software design and engineering
* [enhancement_two](https://github.com/ncperry95/CS-360-Portfolio-InventoryApp/tree/enhancement_two): algorithms and data structures
* [enhancement_three](https://github.com/ncperry95/CS-360-Portfolio-InventoryApp/tree/enhancement_three): databases, which is the same code as main

## What changed from the original

### Software design and engineering

* Split the app into screens, ViewModels, and repositories so the activities don't talk to the database directly
* Moved database work off the main thread
* Every item read, update, and delete now checks that the item belongs to the logged in user. The original only checked the item id, so one user could reach another user's items if they knew the id.
* Replaced unsalted SHA256 password hashes with PBKDF2WithHmacSHA256 using 100,000 iterations and a random 16 byte salt, checked with a constant time comparison. Accounts that still have an old hash can log in and get rehashed right then.
* Removed a fallback that saved the plain password if hashing failed
* Raised minSdk from 24 to 26, since that's where PBKDF2WithHmacSHA256 becomes available

### Algorithms and data structures

* Name search uses a prefix trie, so results narrow as you type without scanning the whole list each time
* Sort options are built from comparators
* Low stock items are tracked with a heap based priority queue
* The grid uses DiffUtil, so only the items that actually changed get redrawn

### Databases

* Moved from a hand written SQLiteOpenHelper class to Room 2.8.5
* Added a real migration from the old version 1 database to version 2 instead of dropping every table on upgrade. It keeps existing users and items, sets any negative quantity to 0, removes items whose owner no longer exists, and keeps the id counters where they were.
* Added a unique index on username and an index on owner_user_id
* Added a foreign key from inventory.owner_user_id to users.id with cascade delete
* Added triggers that reject negative quantities, even from code that skips the repository
* Room exports the schema to app/schemas, so schema changes get tracked like any other source file
* The database is excluded from cloud backups and device to device transfer

## Tests

There are 84 tests and all of them pass.

* 57 unit tests run on the JVM and cover password hashing, password rules, the trie, sorting, the low stock queue and rule, and the item model
* 27 instrumented tests run on an emulator and cover the DAOs, both repositories, and the version 1 to 2 migration. They include checks that one user can't read, change, or delete another user's item.

From the InventoryApp folder:

    gradlew testDebugUnitTest
    gradlew connectedDebugAndroidTest

The second command needs an emulator or a connected device.

## Building

Open the InventoryApp folder in Android Studio. The project was built and tested with JDK 17, compileSdk and targetSdk 36, and minSdk 26, and last tested on a Pixel 7 emulator running API 34.

## License

MIT. See the LICENSE file.

## Original CS 360 reflection

I wrote this for my CS 360 portfolio before any of the capstone work, so it describes the original version of the app, including the DatabaseHelper class that Room has since replaced.

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
