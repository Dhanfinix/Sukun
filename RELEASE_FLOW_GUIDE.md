# Sukun Git Branching & Release Workflow Guideline

To ensure stable releases and a clean working environment, the Sukun project follows a simplified mapping of git branches to Google Play deployment environments.

## The Three Branches

1. **`main`** 
   - Strict representation of **Production / Live to Users**.
   - Accepts Pull Requests *only* from `internal`.
   - Merging to `main` instantly triggers a **Play Store Production Release** and creates a GitHub Release with an auto-generated version tag (e.g., `v1.2.0`).

2. **`internal`**
   - Representation of the **Internal Testing Track**.
   - Accepts Pull Requests *only* from `development` or hotfix branches.
   - Merging to `internal` instantly triggers a **Play Store Internal Release** (for testing on real devices before pushing to the public).

3. **`development`**
   - The primary active coding branch.
   - New features and transit branches (`feature/*`, `fix/*`, `chore/*`) are branched off from here.
   - Regularly rebased with `main`/`internal` to stay up to date.

## Step-by-Step Developer Flow

### 1. Daily Development
When building a new feature or fixing a bug:
1. `git checkout development`
2. `git pull origin development`
3. `git checkout -b feature/new-awesome-feature`
4. Code, commit, and push.
5. Create a Pull Request against `development`.

### 2. Testing Your Changes on a Real Device (Internal Release)
Once a batch of features is merged into `development` and is ready for real-world testing:
1. Ensure the `versionCode` is bumped by +1 in `app/build.gradle.kts`.
2. Update the `whatsnew/whatsnew-en-US` and `whatsnew-id-ID` text files with the new features listed.
3. Open a Pull Request from `development` → `internal`.
4. Once merged, the GitHub Action (`release-internal.yml`) builds the AAB and ships it to the **Internal Track** on Google Play Console.

### 3. Shipping to the Public (Production Release)
Once the internal testers confirm the app is stable and working:
1. Check that the `versionName` (e.g., `1.2.0`) in `app/build.gradle.kts` exactly reflects the update.
2. Open a Pull Request from `internal` → `main`.
3. Once merged, the GitHub Action (`release-production.yml`) triggers:
   - Builds the final signed AAB.
   - Pushes it to the **Production Track** on Google Play Console.
   - Creates a **GitHub Release** and automatically tags the repo with `v1.2.0` based on the version name.

## Play Store "What's New" Notes
The `whatsnew/` folder dictates what text is presented as the update changelog in the Play Store. It supports localization. Keep descriptions concise and under 500 characters so Google Play accepts them. Update these files before merging PRs.
