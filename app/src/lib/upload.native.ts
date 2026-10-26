import * as ImagePicker from "expo-image-picker";
import { Directory, File, Paths } from "expo-file-system";

export async function pickSquarePhoto(): Promise<string | null> {
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ["images"],
    allowsEditing: true,
    aspect: [1, 1],
    quality: 0.6,
  });
  if (result.canceled || !result.assets?.length) return null;
  return result.assets[0].uri;
}

/** Copies a picked photo into the app's own folder so it survives cache clean-ups. */
export async function savePhoto(localUri: string, name: string): Promise<string> {
  const dir = new Directory(Paths.document, "photos");
  if (!dir.exists) dir.create();
  const dest = new File(dir, `${name}.jpg`);
  if (dest.exists) dest.delete();
  await new File(localUri).copy(dest);
  return dest.uri;
}
