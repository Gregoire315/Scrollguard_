# ScrollGuard

Application Android minimaliste : détecte si Instagram ou X (via `x.com` /
`twitter.com`) sont ouverts **dans Chrome**, et envoie une notification avec
un message motivant toutes les 5 minutes tant que tu restes dessus. Les
rappels s'arrêtent dès que tu quittes le site.

Aucune donnée n'est envoyée où que ce soit : tout tourne localement sur le
téléphone.

## Comment obtenir le fichier .apk (aucun code à écrire)

Je n'ai pas pu compiler l'APK moi-même (pas d'accès au SDK Android dans mon
environnement), donc voici comment le faire compiler gratuitement par
GitHub, en 10 minutes, sans rien installer sur ton ordinateur.

### 1. Créer un dépôt GitHub

1. Va sur [github.com](https://github.com) et crée un compte gratuit si tu
   n'en as pas.
2. Clique sur **New repository**, nomme-le `scrollguard`, laisse-le en
   **Public** ou **Private** (peu importe), ne coche aucune case
   d'initialisation, puis **Create repository**.

### 2. Envoyer ce projet dans le dépôt

Sur la page du nouveau dépôt vide, GitHub propose un lien
**"uploading an existing file"** — clique dessus, puis glisse-dépose
**tout le contenu** de ce dossier `ScrollGuard/` (pas le dossier
lui-même, son contenu : `app/`, `.github/`, `build.gradle.kts`, etc.),
et valide avec **Commit changes**.

### 3. Lancer la compilation

1. Dans le dépôt, va dans l'onglet **Actions**.
2. Le workflow **"Build APK"** démarre automatiquement après l'envoi des
   fichiers (comptez 3 à 5 minutes). S'il ne démarre pas seul, clique
   dessus puis **Run workflow**.
3. Une fois le workflow terminé (coche verte ✅), clique dessus, puis en
   bas de la page, télécharge l'archive **ScrollGuard-debug-apk**.
4. Dézippe-la : elle contient `app-debug.apk`.

### 4. Installer sur le Galaxy A56

1. Transfère `app-debug.apk` sur ton téléphone (par mail à toi-même,
   Google Drive, ou câble USB).
2. Ouvre le fichier depuis le téléphone. Android va demander
   l'autorisation d'installer des applications inconnues depuis
   l'application que tu utilises (Fichiers, Gmail...) — accepte.
3. Installe l'application.

### 5. Configurer l'application (une seule fois)

1. Ouvre **ScrollGuard**.
2. Appuie sur **"1. Activer le service d'accessibilité"**.
3. **Point important** : Android bloque par défaut l'accès à
   l'accessibilité pour les applications installées hors Play Store,
   pour des raisons de sécurité (l'accessibilité peut lire l'écran).
   Si tu ne vois pas ScrollGuard dans la liste, ou si l'interrupteur
   reste grisé :
   - Va dans **Paramètres → Applications → ScrollGuard**
   - Ouvre le menu **⋮** en haut à droite
   - Choisis **"Autoriser les paramètres restreints"**
   - Reviens dans Paramètres → Accessibilité → Applications
     installées → ScrollGuard, et active l'interrupteur.
4. Reviens dans l'app ScrollGuard, appuie sur
   **"2. Autoriser les notifications"** et accepte.

C'est prêt. Ouvre Instagram ou X dans Chrome pour tester (le premier
rappel arrive après 5 minutes).

## Personnaliser les messages

Les 10 messages sont dans
`app/src/main/java/com/gregoire/scrollguard/ReminderManager.kt`
(liste `messages`). Modifie-les directement dans l'éditeur de fichier
de GitHub (icône crayon), commit, et le workflow recompile
automatiquement une nouvelle APK à télécharger dans **Actions**.

Pour changer l'intervalle de 5 minutes, modifie la constante
`INTERVAL_MS` dans le même fichier.

## Limites connues (v1)

- La détection lit la barre d'adresse de Chrome. Si une future version
  de Chrome change l'identifiant technique de ce champ, la détection
  bascule sur un repli qui scanne le texte visible de la page — moins
  précis, donc des faux positifs/négatifs occasionnels sont possibles.
  Si ça arrive, dis-le moi et j'ajuste le code.
- Ne fonctionne que dans Chrome (pas dans d'autres navigateurs), et
  seulement pour les onglets, pas les widgets ou raccourcis.
- L'app n'a pas d'icône personnalisée (icône système par défaut) —
  purement cosmétique, sans impact sur le fonctionnement.
