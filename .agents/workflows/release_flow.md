---
description: How to release Sukun to internal and production using the 3-branch strategy
---

# Sukun Release Flow

Sukun uses a 3-branch strategy for tracking deployments to Google Play:

1. **`development`**: Active coding branch.
2. **`internal`**: Maps to the **Internal Testing Track** on Google Play.
3. **`main`**: Maps to the **Production Track** on Google Play.

## 1. Daily Development
1. Always base new work off the `development` branch.
   ```bash
   git checkout development
   git pull origin development
   git checkout -b feature/my-new-feature
   ```
2. Commit your code and open a Pull Request to `development`.

## 2. Releasing to Internal Testing
When `development` has a batch of features ready for real-device testing:
1. **Bump Version Code:** Update `versionCode` in `app/build.gradle.kts`.
2. **Update Release Notes:** Edit `whatsnew/whatsnew-en-US` and `whatsnew/whatsnew-id-ID` (max 500 characters).
3. **Merge:** Open a PR from `development` -> `internal`.
4. **Deploy:** Merging to `internal` automatically triggers the GitHub Action to build and upload the AAB to the Play Store Internal track.

## 3. Releasing to Production
When the internal build is verified and ready for public release:
1. **Check Version Name:** Ensure `versionName` in `app/build.gradle.kts` matches the release (e.g., `1.2.0`).
2. **Merge:** Open a PR from `internal` -> `main`.
3. **Deploy:** Merging to `main` automatically triggers the GitHub Action to build and upload the AAB to the Play Store Production track. It will also automatically create a GitHub Release and `v*` tag.
