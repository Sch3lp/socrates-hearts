# Model Hearts in Kotlin

This repository contains tests and some code structure for a game in the [Hearts](https://en.wikipedia.org/wiki/Hearts_(card_game)) family.
Tests are provided, it's up to you to make them green and model the game in the process.

This repository is the hard work of [Enver Bral](https://gitlab.com/enverbral). Find his original repository [over here on GitLab](https://gitlab.com/enverbral/socrates-hearts).

## How to import this project

### Using Intellij IDEA

Open this project in Intellij. It should automatically start importing the project as a gradle project.
I also recommend using the following settings:
* Set project SDK to [JDK 22](https://jdk.java.net/22/) (you might have to install this specific version if you don't have it already)
* Under Gradle settings:
  * Build and run using **Gradle**
  * Test using **Gradle**
  * Set Distribution to **Wrapper**
  * Set Gradle JVM to **Project SDK**

If you encounter any issues during import, then use the recommended settings and reload the gradle project.

Verify that everything is working by running the gradle `test` task under `Tasks > verification`

### Using command line

Be sure to use [JDK 22](https://jdk.java.net/22/), e.g. by setting `JAVA_HOME` or using [sdkman](https://sdkman.io/).

Use the included gradle wrapper to trigger the test task: `./gradlew test`

## Which rules we're implementing

The tests are based on [Microsoft Hearts](https://en.wikipedia.org/wiki/Microsoft_Hearts) with the following rules:
* The American version of [Black Lady](https://en.wikipedia.org/wiki/Black_Lady)
* with [modern rules](https://en.wikipedia.org/wiki/Black_Lady#Modern_rules_%E2%80%93_Morehead_(2001))
* for exactly 4 players (no more, no less)
* with the alternative rule that 2♣️ leads
* without [Shooting the moon](https://en.wikipedia.org/wiki/Black_Lady#Shooting_the_moon)

If you're done modeling the base game, then you can also implement the additional rule of [Shooting the moon](https://en.wikipedia.org/wiki/Black_Lady#Shooting_the_moon)

## Some things to keep in mind during implementation

* The application is written as though any player can execute any command at any time
  * You’ll have to check that players aren’t “cheating”
  * E.g. playing a card they don’t have in their hand
  * E.g. playing a card when it’s not their turn
* The tests require that you make the following configurable in some way:
  * How cards are dealt (some tests deal fixed cards to fixed players)
  * Which passing direction is used in which round (some tests were easier to implement when passing direction is fixed for every round)
* Focus on modeling the domain and the rules


