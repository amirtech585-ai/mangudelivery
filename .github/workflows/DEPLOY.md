# ManguDelivery: Free Demo Deployment

This guide deploys the demo for ManguDelivery. It uses free tiers: Neon (PostgreSQL), Render (backend API), Netlify (admin web), and Firebase App Distribution (Android app).

Free tiers change. Check each provider's current limits before you start.

## What you need

- A GitHub account, with this project pushed to a repository (Render and Netlify both deploy from GitHub)
- Accounts on Neon, Render, Netlify, and Firebase (Google account)
- On your computer: Git, Flutter SDK (for the Android build), and OpenSSL (or any way to generate a random string)

## Step 1: Push the project to GitHub

```bash
cd ManguDelivery
git init
git add .
git commit -m "ManguDelivery demo"
git branch -M main
git remote add origin https://github.com/YOUR-USER/mangudelivery.git
git push -u origin main
```

Check that `.env` files and keystores are not in the commit (the `.gitignore` excludes them).

## Step 2: Create the database (Neon)

1. Sign in at neon.tech and create a project named `mangudelivery`.
2. Copy the connection string. It looks like `postgresql://user:password@host/dbname?sslmode=require`.
3. Convert it for Spring Boot. Spring needs the JDBC form, and the user and password go in separate variables:
   - `DB_URL` = `jdbc:postgresql://host/dbname?sslmode=require` (no user or password in the URL)
   - `DB_USER` = the user
   - `DB_PASSWORD` = the password
4. If the password contains special characters such as `@`, `/`, `#`, or `%`, use it exactly as Neon shows it. Do not URL-encode it in `DB_PASSWORD`.

Tables are created automatically the first time the API starts.

## Step 3: Generate the secrets

On your computer:

```bash
openssl rand -base64 48     # use the output as JWT_SECRET
```

Choose a strong `ADMIN_PASSWORD` (12 characters or more). Keep both values in your password manager.

## Step 4: Deploy the backend (Render)

1. Sign in at render.com and choose **New > Blueprint**. Select your GitHub repository. Render reads `render.yaml`.
2. Render asks for the values marked as sync false. Enter:

| Variable | Value |
|---|---|
| `JWT_SECRET` | from Step 3 |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | from Step 2 |
| `ADMIN_EMAIL` | your administrator email |
| `ADMIN_PASSWORD` | from Step 3 |
| `CORS_ALLOWED_ORIGINS` | leave a placeholder for now, for example `http://localhost:5173`; update it in Step 5 |

3. Apply the blueprint and wait for the build to finish. The first build takes several minutes.
4. Copy the service URL, for example `https://mangudelivery-api.onrender.com`.
5. Test it. Open `https://YOUR-API/api/auth/login` in a browser. A `405 Method Not Allowed` response means the server is running (the endpoint expects POST). A blank page or a connection error means the deploy has failed; check the Render logs.

If the blueprint fails, create a **Web Service** manually: runtime Docker, root directory `backend`, Dockerfile path `./Dockerfile`, and the same environment variables.

**Note:** on the free plan the service sleeps after about 15 minutes without traffic. The first request after that takes around a minute. Warn testers about this.

## Step 5: Deploy the admin web (Netlify)

1. Sign in at netlify.com and choose **Add new site > Import an existing project**. Select the repository.
2. Netlify reads `netlify.toml` (base `admin-web`, build `npm run build`, publish `admin-web/dist`). Confirm the settings.
3. Under **Site configuration > Environment variables**, add:
   - `VITE_API_BASE_URL` = your Render URL from Step 4, for example `https://mangudelivery-api.onrender.com`
4. Deploy the site. Copy the Netlify address, for example `https://mangudelivery-admin.netlify.app`.
5. Go back to Render, set `CORS_ALLOWED_ORIGINS` to the Netlify address, and let the service redeploy.
6. Open the Netlify address and sign in with `ADMIN_EMAIL` and `ADMIN_PASSWORD`.

Because `VITE_API_BASE_URL` is set at build time, rebuild the site (Netlify > Deploys > Trigger deploy) if the API address changes.

## Step 6: Build the Android app

The source has no Android or iOS folders. Generate them once:

```bash
cd mobile-app
flutter create --org com.mangu --project-name mangu_delivery_app .
flutter pub get
flutter analyze
```

Set the application ID to `com.mangu.delivery` in `android/app/build.gradle` (both `applicationId` and `namespace`), and set `minSdk` to 26.

Build the APK with your Render URL:

```bash
flutter build apk --release --dart-define=API_BASE_URL=https://mangudelivery-api.onrender.com
```

The APK is at `build/app/outputs/flutter-apk/app-release.apk`.

## Step 7: Share the Android app (Firebase App Distribution)

1. Go to console.firebase.google.com, create a project, and add an Android app with package name `com.mangu.delivery`.
2. Open **Release & Monitor > App Distribution**, upload the APK, and add tester email addresses.
3. Testers get an email with an install link. They need to allow installs from the link (see the device launch guide, Section 6.1).

For iPhone testers, use TestFlight, which needs an Apple Developer account and a Mac for the build.

## Step 8: Prepare the demo data

Create the demo accounts through the app, then approve them in the admin web:

1. Register one seller and one rider on a test device (or the Android emulator).
2. In the admin web, open **Applications**, review each one, and approve it.
3. Register a buyer. Buyers are active immediately.
4. Add two or three products as the seller, and mark the seller's stock.

Use made-up names, phone numbers, and ID numbers only.

## Step 9: Check the demo before sharing it

Run the acceptance test in Section 8 of the Device Launch Guide, or at least these steps:

- The buyer places a cash order and sees a 4-digit PIN.
- The seller accepts the order and marks it ready.
- The rider accepts it, confirms pickup, starts the trip, and delivers with the PIN.
- The admin sees the order as Completed on the Orders page, with the commission shown.

## Troubleshooting

| Problem | Likely cause and fix |
|---|---|
| Render build fails on `dependency:go-offline` | Check the Render build log. Often a temporary network issue; redeploy. |
| API exits at startup with "JWT_SECRET must be set" | `JWT_SECRET` is missing or shorter than 32 bytes. Set it in Render and redeploy. |
| API exits with a database connection error | `DB_URL` must be the JDBC form with no user or password in the URL. Check `sslmode=require`. |
| Admin web opens but login says "Cannot reach the server" | `VITE_API_BASE_URL` is wrong (rebuild needed), or the API is asleep (wait a minute and retry). |
| Admin login fails with a CORS error in the browser console | `CORS_ALLOWED_ORIGINS` does not match the Netlify address exactly, including `https://` and no trailing slash. |
| Admin login fails with "Invalid email, password, or role" | The admin account is created only at first startup from `ADMIN_EMAIL` and `ADMIN_PASSWORD`. If you changed them later, the old account still exists; use the original password or reset the database. |
| Android app says "Cannot reach the server" | The APK was built without `--dart-define=API_BASE_URL`, so it uses the emulator address. Rebuild. |
| First request after a pause is very slow | Free Render sleep. Expected; open the API link once before a demo. |

## Limits of the free demo

- The database and API may pause when idle. Plan to wake them shortly before a demo.
- Data may be lost when a free database is reset or deleted. Keep the demo data list so you can recreate it.
- The demo is not secure enough for real customers, real money, or real personal data. See the launch guide before any live use.
