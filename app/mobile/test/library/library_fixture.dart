import 'dart:async';
import 'package:dartz/dartz.dart';
import 'package:tellpal_mobile_poc/app/library_configuration.dart';
import 'package:tellpal_mobile_poc/core/error/failure.dart';
import 'package:tellpal_mobile_poc/features/library/domain/entity/library_catalog.dart';
import 'package:tellpal_mobile_poc/features/library/domain/repository/library_repository.dart';

LibraryConfiguration configuration({
  String language = 'tr',
  Duration wait = const Duration(seconds: 15),
}) => LibraryConfiguration(
  baseUrl: Uri.parse('http://localhost:8082'),
  idToken: 'test-token',
  context: LibraryContext(language, 'test-account'),
  catalogWait: wait,
  artworkWait: wait,
);
LibraryCatalog catalog(
  String language, {
  bool empty = false,
  String audience = 'NON_SUBSCRIBER',
}) => LibraryCatalog(
  schemaVersion: 1,
  requestedLanguage: language,
  audience: audience,
  previewLimit: 5,
  rows: empty
      ? []
      : List.generate(
          4,
          (row) => LibraryRow(
            categoryId: row + 1,
            slug: 'category-$row',
            name: language == 'tr'
                ? 'Uzun Türkçe Hikâyeler ve Harika Maceralar'
                : language == 'pt'
                ? 'Histórias e aventuras maravilhosas para toda a família'
                : 'Wonderful long stories and adventures',
            description: language == 'tr'
                ? 'Hayal gücünü keşfeden, birlikte okunacak hikâyeler.'
                : language == 'pt'
                ? 'Histórias para explorar a imaginação e ler juntos.'
                : 'Stories to explore imagination and read together.',
            displayOrder: row,
            contentLanguage: language,
            items: List.generate(
              3,
              (item) => LibraryPreview(
                contentId: row * 10 + item + 1,
                externalKey: 'story-$row-$item',
                type: 'STORY',
                experienceType: 'READING',
                contentLanguage: language,
                title: language == 'tr'
                    ? 'Ay Işığında Birlikte Yolculuğa Çıkan Küçük Kahramanların Hikâyesi'
                    : language == 'pt'
                    ? 'A História dos Pequenos Amigos que Viajaram Juntos pela Floresta ao Luar'
                    : 'The Little Friends Who Journeyed Together Through the Moonlit Forest',
                isFree: item == 0,
                isLocked: item != 0,
                covers: const LibraryCovers(),
              ),
            ),
          ),
        ),
);

class ControlledRepository implements LibraryRepository {
  final calls = <LibraryContext>[];
  final responses = <Completer<Either<Failure, LibraryCatalog>>>[];
  @override
  Future<Either<Failure, LibraryCatalog>> fetch(LibraryContext context) {
    calls.add(context);
    final response = Completer<Either<Failure, LibraryCatalog>>();
    responses.add(response);
    return response.future;
  }
}
