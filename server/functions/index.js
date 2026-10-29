// Askday server code (Cloud Functions). Deployed by the "Deploy server" GitHub workflow.
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
setGlobalOptions({ region: "asia-south1", maxInstances: 10 });
const db = admin.firestore();

// Phone numbers are compared as SHA-256 hashes of "pepper + E.164 number" (for example +919876543210).
// The app hashes contacts the same way, so raw numbers never leave the phone.
const PEPPER = "askday-contacts-v1:";
const hash = (e164) => crypto.createHash("sha256").update(PEPPER + e164).digest("hex");

function requireUser(req) {
  if (!req.auth || !req.auth.uid) throw new HttpsError("unauthenticated", "Sign in first.");
  return req.auth.uid;
}

/** Registers your own verified phone number (from your sign-in), so friends can find you. */
exports.registerPhone = onCall(async (req) => {
  const uid = requireUser(req);
  const phone = req.auth.token.phone_number;
  if (!phone) return { registered: false }; // guests have no number
  await db.collection("phoneHashes").doc(hash(phone)).set({ uid, at: admin.firestore.FieldValue.serverTimestamp() });
  return { registered: true };
});

/** Takes hashed contact numbers and returns which belong to Askday accounts. */
exports.matchContacts = onCall(async (req) => {
  const uid = requireUser(req);
  const raw = Array.isArray(req.data && req.data.hashes) ? req.data.hashes : [];
  const hashes = [...new Set(raw.filter((h) => typeof h === "string" && /^[0-9a-f]{64}$/.test(h)))].slice(0, 2000);
  const found = [];
  for (let i = 0; i < hashes.length; i += 300) {
    const snaps = await db.getAll(...hashes.slice(i, i + 300).map((h) => db.collection("phoneHashes").doc(h)));
    for (const s of snaps) if (s.exists && s.get("uid") !== uid) found.push({ hash: s.id, uid: s.get("uid") });
  }
  if (found.length === 0) return { matches: [] };
  const users = await db.getAll(...found.map((m) => db.collection("users").doc(m.uid)));
  const matches = found
    .map((m, i) => ({
      hash: m.hash,
      uid: m.uid,
      name: users[i].get("name") || "",
      username: users[i].get("username") || null,
      photoUrl: users[i].get("photoUrl") || null,
    }))
    .filter((m) => m.name);
  return { matches };
});

/** Removes your phone number from matching (used when deleting your account). */
exports.unregisterPhone = onCall(async (req) => {
  requireUser(req);
  const phone = req.auth.token.phone_number;
  if (phone) await db.collection("phoneHashes").doc(hash(phone)).delete();
  return { ok: true };
});
