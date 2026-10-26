import * as ImagePicker from "expo-image-picker";

// Web version: the picked photo comes back as a data URL, which we keep as is.
// The phone version (upload.native.ts) copies it into the app's own storage.

export async function pickSquarePhoto(): Promise<string | null> {
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ["images"],
    allowsEditing: true,
    aspect: [1, 1],
    quality: 0.5,
  });
  if (result.canceled || !result.assets?.length) return null;
  return result.assets[0].uri;
}

export async function savePhoto(localUri: string, _name: string): Promise<string> {
  return localUri;
}
