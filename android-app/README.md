# Railroad Resources Android app

The app is built by `.github/workflows/build-android-app.yml`. Its bundled schedule is copied from the repository's canonical `jobs.csv` during every build, while the installed app checks the same file on GitHub for later schedule-only updates.

To publish an app update, update the Android version in `AndroidManifest.xml` and `assets/index.html`, commit the change, and push a matching `v*` tag. The workflow builds, signs, verifies, and attaches `Railroad-Resources.apk` to the GitHub release.

The signing key and passwords are stored only as encrypted GitHub Actions secrets.
