import 'dart:io';

import 'package:flutter/material.dart';

import 'story_manifest_client.dart';
import 'story_player_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  PaintingBinding.instance.imageCache.maximumSizeBytes = 24 * 1024 * 1024;
  runApp(const TellPalMediaPoc());
}

class TellPalMediaPoc extends StatelessWidget {
  const TellPalMediaPoc({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'TellPal Medya PoC',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF6254A4)),
      ),
      home: const _ConnectionPage(),
    );
  }
}

class _ConnectionPage extends StatefulWidget {
  const _ConnectionPage();

  @override
  State<_ConnectionPage> createState() => _ConnectionPageState();
}

class _ConnectionPageState extends State<_ConnectionPage> {
  final _backendController = TextEditingController(
    text: Platform.isAndroid ? 'http://10.0.2.2:8080' : 'http://localhost:8080',
  );
  final _tokenController = TextEditingController();

  @override
  void dispose() {
    _backendController.dispose();
    _tokenController.dispose();
    super.dispose();
  }

  void _openStory() {
    final baseUrl = Uri.tryParse(_backendController.text.trim());
    final token = _tokenController.text.trim();
    if (baseUrl == null ||
        !{'http', 'https'}.contains(baseUrl.scheme) ||
        baseUrl.host.isEmpty ||
        token.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Geçerli bir backend adresi ve test token’ı girin.'),
        ),
      );
      return;
    }

    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => StoryPlayerPage(
          client: StoryManifestClient(baseUrl: baseUrl, bearerToken: token),
          contentId: 1,
          languageCode: 'tr',
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('TellPal Medya PoC')),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: ListView(
              padding: const EdgeInsets.all(24),
              shrinkWrap: true,
              children: [
                Text(
                  'Haberci Nota',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 8),
                const Text('Türkçe · 14 sayfa · korumalı medya denemesi'),
                const SizedBox(height: 28),
                TextField(
                  controller: _backendController,
                  keyboardType: TextInputType.url,
                  autocorrect: false,
                  decoration: const InputDecoration(
                    labelText: 'Backend adresi',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _tokenController,
                  obscureText: true,
                  autocorrect: false,
                  decoration: const InputDecoration(
                    labelText: 'Test token’ı (Bearer olmadan)',
                    border: OutlineInputBorder(),
                  ),
                  onSubmitted: (_) => _openStory(),
                ),
                const SizedBox(height: 12),
                const Text(
                  'Token yalnızca bu çalışma sırasında bellekte tutulur.',
                ),
                const SizedBox(height: 24),
                FilledButton.icon(
                  onPressed: _openStory,
                  icon: const Icon(Icons.menu_book),
                  label: const Text('Hikâyeyi aç'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
