const functions = require("firebase-functions");
const admin = require("firebase-admin");
if (!admin.apps.length) admin.initializeApp();
const db = admin.firestore();

/** Helper: pastikan caller adalah admin */
async function assertAdmin(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Login dulu.");
  }
  const callerUid = context.auth.uid;
  // cek claim dulu
  const token = context.auth.token || {};
  if (token.role === "admin") return;

  // fallback: cek Firestore role
  const doc = await db.collection("users").doc(callerUid).get();
  const role = (doc.exists && doc.get("role")) || "";
  if (role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", "Hanya admin.");
  }
}

/** Buat user baru + set password + role + dok profil */
exports.adminCreateUser = functions.https.onCall(async (data, context) => {
  await assertAdmin(context);
  const { email, password, name = "", role = "staff" } = data || {};
  if (!email || !password) {
    throw new functions.https.HttpsError("invalid-argument","email & password wajib.");
  }

  // buat user auth
  const userRec = await admin.auth().createUser({
    email, password, displayName: name
  });

  // set custom claims
  await admin.auth().setCustomUserClaims(userRec.uid, { role });

  // tulis profil ke Firestore
  const profile = {
    uid: userRec.uid,
    email,
    name: name || email.split("@")[0],
    role,
    isActive: true,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  await db.collection("users").doc(userRec.uid).set(profile, { merge: true });

  return { uid: userRec.uid };
});

/** Update user (opsional): name, role, password */
exports.adminUpdateUser = functions.https.onCall(async (data, context) => {
  await assertAdmin(context);
  const { uid, name, role, password } = data || {};
  if (!uid) throw new functions.https.HttpsError("invalid-argument","uid wajib.");

  if (name || password) {
    const upd = {};
    if (name) upd.displayName = name;
    if (password) upd.password = password;
    await admin.auth().updateUser(uid, upd);
  }

  if (role) {
    await admin.auth().setCustomUserClaims(uid, { role });
  }

  const patch = {
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  if (name) patch.name = name;
  if (role) patch.role = role;

  await db.collection("users").doc(uid).set(patch, { merge: true });
  return { ok: true };
});
