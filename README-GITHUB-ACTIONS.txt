Stronghold Finder - Fabric 1.16.1 (GitHub Actions build)
-------------------------------------------------------
This version includes a GitHub Actions workflow thatwill build the mod on every push
and save the compiled jar as a workflow artifact you can download.

Steps to get a compiled .jar with zero local setup:
1. Create a new GitHub repository (private or public).
2. Push the contents of this folder to that repository (main/master branch).
   Example commands:
     git init
     git add .
     git commit -m "Add stronghold finder mod"
     git branch -M main
     git remote add origin https://github.com/<your-username>/<repo>.git
     git push -u origin main
3. After the push, go to the "Actions" tab of your repository and open the latest workflow run.
4. When the job completes, expand "Artifacts" and download `stronghold-finder-jar`.
5. Place the jar into your Fabric mods folder for Minecraft 1.16.1.

Notes:
- The workflow runs `./gradlew build`. If gradle wrapper files are missing, Gradle will attempt to download the wrapper.
- If the build fails due to network/maven issues, paste the workflow logs here and I will help fix them.
