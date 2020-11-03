import 'dart:ui';

class HexColor extends Color {
  static int _getColorFromHex(String hexColor) {
    String parsedHexColor = hexColor.toUpperCase().replaceAll("#", "");
    if (parsedHexColor.length == 6) {
      parsedHexColor = "FF$parsedHexColor";
    }
    return int.parse(parsedHexColor, radix: 16);
  }

  HexColor(final String hexColor) : super(_getColorFromHex(hexColor));
}