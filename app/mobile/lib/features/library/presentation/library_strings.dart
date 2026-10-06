class LibraryStrings {
  const LibraryStrings(this.language);
  final String language;
  String _select(String tr, String en, String pt) => language == 'tr'
      ? tr
      : language == 'pt'
      ? pt
      : en;
  String get welcome => _select('Hoş Geldin', 'Welcome!', 'Bem-vindo!');
  String get library => _select('Kitaplık', 'Book', 'Biblioteca');
  String get sleep => _select('Uyku', 'Sleep', 'Sono');
  String get profile => _select('Profil', 'Profile', 'Perfil');
  String get premium => _select('Premium’a Geç', 'Go Premium', 'Seja Premium');
  String get search => _select(
    'En sevdiğin hikâyeleri ara…',
    'Search for your favorite stories…',
    'Procure suas histórias favoritas…',
  );
  String get all => _select('Hepsi', 'All', 'Todas');
  String get loading => _select(
    'Kitaplık yükleniyor',
    'Loading Library',
    'Carregando biblioteca',
  );
  String get empty => _select(
    'Bu dilde henüz hikâye yok.',
    'No stories are available in this language yet.',
    'Ainda não há histórias disponíveis neste idioma.',
  );
  String get error => _select(
    'Kitaplık yüklenemedi. Tekrar deneyebilirsin.',
    'Library could not load. You can try again.',
    'Não foi possível carregar a biblioteca. Tente novamente.',
  );
  String get retry => _select('Tekrar dene', 'Retry', 'Tentar novamente');
  String get artworkError =>
      _select('Kapak yüklenemedi', 'Cover unavailable', 'Capa indisponível');
  String get retryArtwork =>
      _select('Kapağı tekrar yükle', 'Retry cover', 'Recarregar capa');
  String get locked => _select('Kilitli', 'Locked', 'Bloqueada');
}
