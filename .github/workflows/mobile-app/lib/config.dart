class AppConfig {
  /// Override at build time: flutter run --dart-define=API_BASE_URL=https://api.example.com
  /// 10.0.2.2 reaches the host machine from the Android emulator.
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
}
