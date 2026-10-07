class StoryDetailStrings {
  const StoryDetailStrings(this.language);
  final String language;
  String _text(String tr, String en, String pt) => language == 'tr'
      ? tr
      : language == 'pt'
      ? pt
      : en;
  String get back => _text('Geri', 'Back', 'Voltar');
  String get loading =>
      _text('Hikâye yükleniyor', 'Loading story', 'Carregando história');
  String get unavailable => _text(
    'Bu hikâye artık mevcut değil.',
    'This story is unavailable.',
    'Esta história não está disponível.',
  );
  String get error => _text(
    'Hikâye yüklenemedi. Tekrar deneyebilirsin.',
    'Story could not load. You can try again.',
    'Não foi possível carregar a história. Tente novamente.',
  );
  String get admission => _text(
    'Bu hikâyeye erişim doğrulanamadı.',
    'Access to this story could not be verified.',
    'Não foi possível verificar o acesso a esta história.',
  );
  String get retry => _text('Tekrar dene', 'Retry', 'Tentar novamente');
  String get retryArtwork =>
      _text('Kapağı tekrar yükle', 'Retry cover', 'Recarregar capa');
  String get artworkError =>
      _text('Kapak yüklenemedi', 'Cover unavailable', 'Capa indisponível');
  String get start =>
      _text('Hikâyeye Başla', 'Start Story', 'Começar história');
  String get premium => _text('Premium’a Geç', 'Go Premium', 'Seja Premium');
  String get deferred => _text(
    'Yakında kullanılabilir',
    'Available in a later story',
    'Disponível em breve',
  );
  String get locked => _text('Kilitli', 'Locked', 'Bloqueada');
  String get narration => _text('Seslendirme', 'Voiceover', 'Narração');
  String get similar =>
      _text('Benzer Hikâyeler', 'Similar Stories', 'Histórias semelhantes');
  String minutes(int value) =>
      _text('$value Dakika', '$value Minutes', '$value Minutos');
  String age(int value) => _text('$value+ Yaş', '$value+ Age', '$value+ Anos');
  String role(String role) => switch (role) {
    'AUTHOR' => _text('Yazan', 'Written by', 'Escrito por'),
    'NARRATOR' => _text('Seslendiren', 'Narrated by', 'Narrado por'),
    'ILLUSTRATOR' => _text('Resimleyen', 'Illustrated by', 'Ilustrado por'),
    _ => _text('Müzisyen', 'Musician', 'Músico'),
  };
}
