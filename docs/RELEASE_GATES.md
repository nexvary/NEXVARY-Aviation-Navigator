# Release Gates

NEXVARY Aviation Navigator treats release verification as part of the product, not as a manual afterthought.

## Android CI

`Android CI` compiles the debug APK, runs JVM unit tests, and runs Android Lint. The debug APK is uploaded only when the build succeeds.

## UI Release Gate

`UI Release Gate` boots a real Android emulator and runs Compose instrumentation tests against `MainActivity`.

The gate verifies:

- all six top-level destinations are visible and clickable;
- primary navigation targets do not overlap on the phone profile;
- Home quick actions are real click targets;
- every secondary screen has a visible in-app Back control;
- About social controls are real click targets;
- a real post-test screenshot is captured as release evidence.

## Navigation Integrity Gate

`NavigationIntegrityGateTest` specifically targets the recurring dead-button / disconnected-page failure mode.

It:

1. opens Home;
2. clicks Map, Plan, Radar, Traffic, and About;
3. verifies each click reaches the expected destination;
4. verifies every Home internal shortcut reaches its matching destination;
5. verifies global navigation remains available from each page;
6. verifies both Android Back and the visible Back button return secondary pages to Home.

A release is not considered navigation-clean if this test fails.

## Localization contract

The app declares and tests these language codes:

- Arabic (`ar`)
- English (`en`)
- Turkish (`tr`)
- Spanish (`es`)
- German (`de`)
- Italian (`it`)
- French (`fr`)
- Urdu (`ur`)
- Persian (`fa`)
- Russian (`ru`)

Arabic, Urdu, and Persian are forced through the RTL layout contract. Visible text uses start-alignment so those locales render from the right while LTR locales render from the left.

## Security Release Gate

The security gate runs Android Lint, unit tests, a clean debug build, and GitHub CodeQL analysis for Java/Kotlin. The manifest also disables application backup and cleartext HTTP traffic.

No automated gate can prove that software contains zero vulnerabilities. A green gate means the configured static checks and tests found no release-blocking issue in that run.
