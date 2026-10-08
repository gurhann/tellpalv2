import 'dart:io';
import 'package:flutter_test/flutter_test.dart';

Iterable<File> sourceFiles(String directory) => Directory(directory)
    .listSync(recursive: true)
    .whereType<File>()
    .where((file) => file.path.endsWith('.dart'));

Iterable<String> dependencies(File file) sync* {
  final directives = RegExp(r'''(?:import|export)\s+['"]([^'"]+)['"]''');
  for (final directive in directives.allMatches(file.readAsStringSync())) {
    final target = directive.group(1)!;
    if (target.startsWith('package:tellpal_mobile_poc/')) {
      yield Uri.file('lib/').resolve(target.split('/').skip(1).join('/')).path;
    } else if (!target.contains(':')) {
      yield file.uri.resolve(target).path;
    }
  }
}

void main() {
  test(
    'StoryDetail imports its own feature or shared core, never Library internals',
    () {
      for (final file in sourceFiles('lib/features/story_detail')) {
        for (final target in dependencies(file)) {
          expect(
            target.contains('/features/') &&
                !target.contains('/features/story_detail/'),
            false,
            reason: '${file.path} imports another feature through $target',
          );
        }
      }
    },
  );
  test(
    'shared preview DTOs, thumbnail state and rendering have no feature dependency',
    () {
      for (final directory in ['lib/core/preview', 'lib/core/artwork']) {
        for (final file in sourceFiles(directory)) {
          for (final target in dependencies(file)) {
            expect(
              target.contains('/features/'),
              false,
              reason: '${file.path} imports feature-owned code through $target',
            );
          }
        }
      }
    },
  );
}
