/// A logged-in account on this device. One device stores one session at a time.
class Session {
  const Session({
    required this.token,
    required this.userId,
    required this.role,
    required this.status,
  });

  final String token;
  final int userId;
  final String role;
  final String status;

  factory Session.fromJson(Map<String, dynamic> json) {
    return Session(
      token: json['token'] as String,
      userId: (json['userId'] as num).toInt(),
      role: json['role'] as String,
      status: json['status'] as String,
    );
  }

  Map<String, dynamic> toJson() => {
        'token': token,
        'userId': userId,
        'role': role,
        'status': status,
      };
}
