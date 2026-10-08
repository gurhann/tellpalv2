import 'package:flutter/material.dart';

class AppColors {
  static const canvas = Color(0xFFF5F7F9),
      primary = Color(0xFF5462DE),
      ink = Color(0xFF2C323F),
      muted = Color(0xFF445064),
      subtle = Color(0xFF6A81A6),
      placeholder = Color(0xFFE6ECF4),
      white = Colors.white,
      bannerStart = Color(0xFF1F5EFF),
      bannerEnd = Color(0xFF6420F6),
      storyNarrationAccent = Color(0xFFE2123F);
}

class AppTypography {
  static const greeting = TextStyle(
    fontFamily: 'Lexend',
    fontSize: 22,
    fontWeight: FontWeight.w600,
    color: AppColors.ink,
  );
  static const section = TextStyle(
    fontFamily: 'Lexend',
    fontSize: 20,
    fontWeight: FontWeight.w600,
    color: AppColors.ink,
  );
  static const description = TextStyle(
    fontFamily: 'Lexend',
    fontSize: 16,
    fontWeight: FontWeight.w500,
    color: AppColors.muted,
  );
  static const label = TextStyle(
    fontFamily: 'Lexend',
    fontSize: 14,
    fontWeight: FontWeight.w500,
    color: AppColors.ink,
  );
  static ThemeData theme() => ThemeData(
    fontFamily: 'Lexend',
    scaffoldBackgroundColor: AppColors.canvas,
    colorScheme: ColorScheme.fromSeed(seedColor: AppColors.primary),
    useMaterial3: true,
  );
}
