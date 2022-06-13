// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/// The possible focus modes that can be set for a camera.
enum FocusPointMode {
  /// Automatically determine focus settings.
  auto,

  /// New algorithm (working well on non Samsung devices)
  newAlgorithm,

  /// For Android < 12 & Samsung devices
  oldAlgorithm,
}

/// Returns the focus mode as a String.
String serializeFocusPointMode(FocusPointMode? focusPointMode) {
  if (focusPointMode == null) {
    return 'auto';
  }

  switch (focusPointMode) {
    case FocusPointMode.auto:
      return 'auto';
    case FocusPointMode.newAlgorithm:
      return 'new';
    case FocusPointMode.oldAlgorithm:
      return 'old';
    default:
      throw ArgumentError('Unknown FocusPointMode value');
  }
}

/// Returns the focus mode for a given String.
FocusPointMode deserializeFocusPointMode(String str) {
  switch (str) {
    case 'auto':
      return FocusPointMode.auto;
    case 'new':
      return FocusPointMode.newAlgorithm;
    case 'old':
      return FocusPointMode.oldAlgorithm;
    default:
      throw ArgumentError('"$str" is not a valid FocusPointMode value');
  }
}
