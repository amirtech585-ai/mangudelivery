import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/order_status.dart';
import '../widgets/ask_reason.dart';
import '../widgets/order_tile.dart';

/// Rider home: jobs to accept, and the rider's own deliveries with the next action for each.
class RiderHomeScreen extends StatefulWidget {
  const RiderHomeScreen({
    super.key,
    required this.api,
    required this.token,
    required this.onLogout,
  });

  final ApiClient api;
  final String token;
  final Future<void> Function() onLogout;

  @override
  State<RiderHomeScreen> createState() => _RiderHomeScreenState();
}

class _RiderHomeScreenState extends State<RiderHomeScreen> {
  late Future<List<dynamic>> _jobs;
  late Future<List<dynamic>> _mine;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    _jobs = widget.api.riderJobs(widget.token);
    _mine = widget.api.riderOrders(widget.token);
  }

  void _reload() => setState(_load);

  Future<void> _run(Future<void> Function() call) async {
    try {
      await call();
      _reload();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _deliver(int id) async {
    final controller = TextEditingController();
    final pin = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Enter the buyer\'s PIN'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          maxLength: 4,
          autofocus: true,
          decoration: const InputDecoration(labelText: '4-digit PIN'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Back')),
          FilledButton(onPressed: () => Navigator.pop(ctx, controller.text.trim()), child: const Text('Confirm')),
        ],
      ),
    );
    controller.dispose();
    if (pin == null || pin.isEmpty) return;
    await _run(() => widget.api.riderAction(widget.token, id, 'deliver', body: {'pin': pin}));
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Rider'),
          bottom: const TabBar(tabs: [Tab(text: 'Available'), Tab(text: 'My deliveries')]),
          actions: [
            IconButton(onPressed: _reload, icon: const Icon(Icons.refresh), tooltip: 'Refresh'),
            IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout), tooltip: 'Log out'),
          ],
        ),
        body: TabBarView(children: [
          _list(_jobs, _availableActions, 'No delivery jobs are available right now.'),
          _list(_mine, _myActions, 'You have no deliveries yet.'),
        ]),
      ),
    );
  }

  Widget _list(Future<List<dynamic>> future, List<Widget> Function(Map<String, dynamic>) actions, String empty) {
    return RefreshIndicator(
      onRefresh: () async => _reload(),
      child: FutureBuilder<List<dynamic>>(
        future: future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return ListView(children: [Padding(
              padding: const EdgeInsets.all(24),
              child: Text(snapshot.error.toString(), textAlign: TextAlign.center),
            )]);
          }
          final items = snapshot.data ?? const [];
          if (items.isEmpty) {
            return ListView(children: [Padding(
              padding: const EdgeInsets.all(24),
              child: Text(empty, textAlign: TextAlign.center),
            )]);
          }
          return ListView(
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: [
              for (final o in items)
                OrderTile(order: o as Map<String, dynamic>, actions: actions(o)),
            ],
          );
        },
      ),
    );
  }

  List<Widget> _availableActions(Map<String, dynamic> o) {
    final id = (o['id'] as num).toInt();
    return [
      FilledButton(
        onPressed: () => _run(() => widget.api.riderAction(widget.token, id, 'accept')),
        child: const Text('Accept job'),
      ),
    ];
  }

  List<Widget> _myActions(Map<String, dynamic> o) {
    final id = (o['id'] as num).toInt();
    switch (o['status']) {
      case 'RIDER_ASSIGNED':
        return [
          FilledButton(
            onPressed: () => _run(() => widget.api.riderAction(widget.token, id, 'pickup')),
            child: const Text('Confirm pickup'),
          ),
          OutlinedButton(
            onPressed: () async {
              final reason = await askReason(context, title: 'Give back this job', hint: 'Why are you releasing it?');
              if (reason == null) return;
              await _run(() => widget.api.riderAction(widget.token, id, 'release', body: {'reason': reason}));
            },
            child: const Text('Release job'),
          ),
        ];
      case 'PICKED_UP':
        return [
          FilledButton(
            onPressed: () => _run(() => widget.api.riderAction(widget.token, id, 'on-the-way')),
            child: const Text('Start trip to buyer'),
          ),
        ];
      case 'ON_THE_WAY':
        return [
          FilledButton(onPressed: () => _deliver(id), child: const Text('Deliver with PIN')),
          OutlinedButton(
            onPressed: () async {
              final reason = await askReason(context, title: 'Delivery not completed', hint: 'What happened?');
              if (reason == null) return;
              await _run(() => widget.api.riderAction(widget.token, id, 'fail', body: {'reason': reason}));
            },
            child: const Text('Could not deliver'),
          ),
        ];
      default:
        return const [];
    }
  }
}
