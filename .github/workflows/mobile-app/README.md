# ManguDelivery Mobile App (Flutter)

One app for Android and iOS. Users choose Buyer, Seller, or Rider when they create an account.
Each account has one role. To use another role, log out and create or log into that account.

## Set up

The `lib/` folder and `pubspec.yaml` are provided. Add the platform folders with Flutter:

```bash
cd mobile-app
flutter create --org com.mangu --project-name mangu_delivery_app .
flutter pub get
```

`flutter create .` keeps the existing `lib/` files and adds `android/` and `ios/`.

Android: allow internet access in `android/app/src/main/AndroidManifest.xml`
(the default debug manifest already includes it, but confirm it).

## Run

```bash
# Emulator, backend on your machine
flutter run

# Physical device or staging server
flutter run --dart-define=API_BASE_URL=https://api.example.com
```

The default API address is `http://10.0.2.2:8080` (the Android emulator's route to your machine).

## Status

| Flow | Status |
|---|---|
| Welcome, buyer/seller/rider registration, login, logout | Implemented |
| Seller/rider pending, rejected, suspended screens | Implemented (resubmission form not yet in the app; the API supports it) |
| Buyer product browsing | Implemented (no cart, checkout, or orders yet) |
| Seller product creation | Implemented (no order handling yet) |
| Rider job list | Placeholder (dispatch not built yet) |
| Maps, GPS, PIN delivery, push notifications, payments | Not implemented |

Tests are not yet written. Run `flutter analyze` before the first build.
